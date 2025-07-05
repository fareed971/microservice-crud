package com.example.consumer;

import com.example.model.UserEvent;
import com.example.model.ErrorEvent;
import com.example.repository.UserEventRepository;
import com.example.repository.ErrorEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UserEventConsumer.class);
    
    @Autowired
    private UserEventRepository userEventRepository;
    
    @Autowired
    private ErrorEventRepository errorEventRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.kafka.topics.processed-events}")
    private String processedEventsTopic;
    
    @Value("${app.kafka.topics.error-events}")
    private String errorEventsTopic;
    
    @Value("${app.kafka.topics.dead-letter}")
    private String deadLetterTopic;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
        include = {Exception.class}
    )
    @KafkaListener(
        topics = "${app.kafka.topics.user-events}",
        groupId = "user-events-group",
        containerFactory = "userEventsKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeUserEvent(
            @Payload UserEvent userEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Consuming user event: userId={}, eventType={}, topic={}, partition={}, offset={}", 
                       userEvent.getUserId(), userEvent.getEventType(), topic, partition, offset);
            
            // Process the user event
            processUserEvent(userEvent);
            
            // Save to MongoDB
            UserEvent savedEvent = userEventRepository.save(userEvent);
            logger.info("User event saved to MongoDB: id={}", savedEvent.getId());
            
            // Send processed event to processed events topic
            sendProcessedEvent(userEvent, "USER_EVENT_PROCESSED");
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            logger.info("User event processed successfully: userId={}", userEvent.getUserId());
            
        } catch (Exception e) {
            logger.error("Error processing user event: userId={}, error={}", 
                        userEvent.getUserId(), e.getMessage(), e);
            
            // Handle the error
            handleProcessingError(userEvent, e, topic, "USER_EVENT_CONSUMER");
            
            // Don't acknowledge - let retry mechanism handle it
            throw new RuntimeException("Failed to process user event", e);
        }
    }

    @KafkaListener(
        topics = "${app.kafka.topics.user-events}.DLT",
        groupId = "user-events-dlt-group"
    )
    public void consumeUserEventDLT(
            @Payload UserEvent userEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            Acknowledgment acknowledgment) {
        
        logger.error("Processing user event from DLT: userId={}, topic={}, error={}", 
                    userEvent.getUserId(), topic, exceptionMessage);
        
        try {
            // Save to error collection in MongoDB
            ErrorEvent errorEvent = new ErrorEvent(
                objectMapper.writeValueAsString(userEvent),
                topic.replace(".DLT", ""),
                exceptionMessage,
                "DLT_PROCESSING",
                "CONSUMER_DLT"
            );
            errorEvent.setRetryCount(3); // Max retries exceeded
            
            errorEventRepository.save(errorEvent);
            logger.info("User event error saved to MongoDB from DLT");
            
            // Send to dead letter topic for manual investigation
            kafkaTemplate.send(deadLetterTopic, userEvent.getUserId(), userEvent);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing user event from DLT: {}", e.getMessage(), e);
        }
    }

    private void processUserEvent(UserEvent userEvent) {
        // Business logic for processing user events
        switch (userEvent.getEventType().toUpperCase()) {
            case "USER_CREATED":
                processUserCreated(userEvent);
                break;
            case "USER_UPDATED":
                processUserUpdated(userEvent);
                break;
            case "USER_DELETED":
                processUserDeleted(userEvent);
                break;
            case "USER_LOGIN":
                processUserLogin(userEvent);
                break;
            case "USER_LOGOUT":
                processUserLogout(userEvent);
                break;
            default:
                logger.warn("Unknown user event type: {}", userEvent.getEventType());
                userEvent.setMetadata("Unknown event type processed");
        }
        
        // Mark as processed
        userEvent.setProcessed(true);
        userEvent.setTimestamp(LocalDateTime.now());
    }

    private void processUserCreated(UserEvent userEvent) {
        logger.info("Processing user created event: userId={}", userEvent.getUserId());
        // Add welcome message metadata
        userEvent.setMetadata("User created successfully. Welcome message sent.");
        
        // Here you could:
        // - Send welcome email
        // - Create user profile
        // - Initialize user preferences
        // - Trigger analytics events
    }

    private void processUserUpdated(UserEvent userEvent) {
        logger.info("Processing user updated event: userId={}", userEvent.getUserId());
        userEvent.setMetadata("User profile updated successfully.");
        
        // Here you could:
        // - Update search index
        // - Invalidate cache
        // - Sync with external systems
        // - Audit trail
    }

    private void processUserDeleted(UserEvent userEvent) {
        logger.info("Processing user deleted event: userId={}", userEvent.getUserId());
        userEvent.setMetadata("User deletion processed. Cleanup tasks initiated.");
        
        // Here you could:
        // - Clean up user data
        // - Cancel subscriptions
        // - Remove from external systems
        // - Data archival
    }

    private void processUserLogin(UserEvent userEvent) {
        logger.info("Processing user login event: userId={}", userEvent.getUserId());
        userEvent.setMetadata("User login processed. Session analytics updated.");
        
        // Here you could:
        // - Update last login time
        // - Security checks
        // - Analytics tracking
        // - Personalization updates
    }

    private void processUserLogout(UserEvent userEvent) {
        logger.info("Processing user logout event: userId={}", userEvent.getUserId());
        userEvent.setMetadata("User logout processed. Session closed.");
        
        // Here you could:
        // - Clean up session data
        // - Update session duration
        // - Security cleanup
        // - Analytics tracking
    }

    private void sendProcessedEvent(UserEvent userEvent, String processingResult) {
        try {
            com.example.model.ProcessedEvent processedEvent = new com.example.model.ProcessedEvent(
                userEvent.getId(),
                userEvent.getEventType(),
                processingResult,
                userEvent.getMetadata(),
                userEvent.getTimestamp()
            );
            
            kafkaTemplate.send(processedEventsTopic, userEvent.getUserId(), processedEvent);
            logger.info("Processed event sent: userId={}, result={}", userEvent.getUserId(), processingResult);
            
        } catch (Exception e) {
            logger.error("Failed to send processed event: userId={}, error={}", 
                        userEvent.getUserId(), e.getMessage());
        }
    }

    private void handleProcessingError(UserEvent userEvent, Exception exception, 
                                     String originalTopic, String processingStage) {
        try {
            String eventJson = objectMapper.writeValueAsString(userEvent);
            
            ErrorEvent errorEvent = new ErrorEvent(
                eventJson,
                originalTopic,
                exception.getMessage(),
                exception.getClass().getSimpleName(),
                processingStage
            );
            
            if (exception.getCause() != null) {
                errorEvent.setStackTrace(getStackTrace(exception));
            }
            
            // Save error to MongoDB
            errorEventRepository.save(errorEvent);
            
            // Send to error topic
            kafkaTemplate.send(errorEventsTopic, "error", errorEvent);
            
            logger.info("Error event created and sent for user event: userId={}", userEvent.getUserId());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize user event for error handling: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to handle processing error: {}", e.getMessage());
        }
    }

    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}