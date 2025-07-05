package com.example.producer;

import com.example.model.UserEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class UserEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(UserEventProducer.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;
    
    @Value("${app.kafka.topics.error-events}")
    private String errorEventsTopic;

    /**
     * Send user event asynchronously with callback handling
     */
    public void sendUserEventAsync(UserEvent userEvent) {
        try {
            String key = userEvent.getUserId();
            
            logger.info("Sending user event asynchronously: userId={}, eventType={}", 
                       userEvent.getUserId(), userEvent.getEventType());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(userEventsTopic, key, userEvent);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("User event sent successfully: topic={}, partition={}, offset={}, key={}", 
                               result.getRecordMetadata().topic(),
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset(),
                               key);
                } else {
                    logger.error("Failed to send user event: key={}, error={}", key, exception.getMessage());
                    handleSendFailure(userEvent, exception);
                }
            });
            
        } catch (Exception e) {
            logger.error("Exception while sending user event: {}", e.getMessage(), e);
            handleSendFailure(userEvent, e);
        }
    }

    /**
     * Send user event synchronously with timeout
     */
    public boolean sendUserEventSync(UserEvent userEvent, long timeoutSeconds) {
        try {
            String key = userEvent.getUserId();
            
            logger.info("Sending user event synchronously: userId={}, eventType={}", 
                       userEvent.getUserId(), userEvent.getEventType());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(userEventsTopic, key, userEvent);
            
            SendResult<String, Object> result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            
            logger.info("User event sent successfully: topic={}, partition={}, offset={}, key={}", 
                       result.getRecordMetadata().topic(),
                       result.getRecordMetadata().partition(),
                       result.getRecordMetadata().offset(),
                       key);
            
            return true;
            
        } catch (TimeoutException e) {
            logger.error("Timeout while sending user event: userId={}, timeout={}s", 
                        userEvent.getUserId(), timeoutSeconds);
            handleSendFailure(userEvent, e);
            return false;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to send user event synchronously: userId={}, error={}", 
                        userEvent.getUserId(), e.getMessage());
            handleSendFailure(userEvent, e);
            return false;
        } catch (Exception e) {
            logger.error("Exception while sending user event synchronously: {}", e.getMessage(), e);
            handleSendFailure(userEvent, e);
            return false;
        }
    }

    /**
     * Send multiple user events in batch
     */
    public void sendUserEventsBatch(java.util.List<UserEvent> userEvents) {
        logger.info("Sending batch of {} user events", userEvents.size());
        
        for (UserEvent userEvent : userEvents) {
            sendUserEventAsync(userEvent);
        }
    }

    /**
     * Send user event with custom partition key
     */
    public void sendUserEventWithPartition(UserEvent userEvent, String partitionKey) {
        try {
            logger.info("Sending user event with custom partition key: userId={}, partitionKey={}", 
                       userEvent.getUserId(), partitionKey);
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(userEventsTopic, partitionKey, userEvent);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("User event with custom partition sent: partition={}, offset={}, key={}", 
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset(),
                               partitionKey);
                } else {
                    logger.error("Failed to send user event with custom partition: key={}, error={}", 
                                partitionKey, exception.getMessage());
                    handleSendFailure(userEvent, exception);
                }
            });
            
        } catch (Exception e) {
            logger.error("Exception while sending user event with custom partition: {}", e.getMessage(), e);
            handleSendFailure(userEvent, e);
        }
    }

    /**
     * Handle send failures by logging and optionally sending to error topic
     */
    private void handleSendFailure(UserEvent userEvent, Throwable exception) {
        try {
            // Convert user event to JSON for error logging
            String eventJson = objectMapper.writeValueAsString(userEvent);
            
            // Log the error
            logger.error("User event send failure - Event: {}, Error: {}", 
                        eventJson, exception.getMessage());
            
            // Optionally send to error topic for manual investigation
            sendToErrorTopic(eventJson, userEventsTopic, exception.getMessage(), "SEND_FAILURE");
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize user event for error handling: {}", e.getMessage());
        }
    }

    /**
     * Send failed event to error topic
     */
    private void sendToErrorTopic(String originalEvent, String originalTopic, 
                                 String errorMessage, String errorType) {
        try {
            com.example.model.ErrorEvent errorEvent = new com.example.model.ErrorEvent(
                originalEvent, originalTopic, errorMessage, errorType, "PRODUCER"
            );
            
            kafkaTemplate.send(errorEventsTopic, "error", errorEvent);
            logger.info("Error event sent to error topic for failed user event");
            
        } catch (Exception e) {
            logger.error("Failed to send error event to error topic: {}", e.getMessage());
        }
    }
}