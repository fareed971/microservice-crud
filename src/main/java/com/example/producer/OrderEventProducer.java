package com.example.producer;

import com.example.model.OrderEvent;
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
public class OrderEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventProducer.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;
    
    @Value("${app.kafka.topics.error-events}")
    private String errorEventsTopic;

    /**
     * Send order event asynchronously with callback handling
     */
    public void sendOrderEventAsync(OrderEvent orderEvent) {
        try {
            String key = orderEvent.getOrderId();
            
            logger.info("Sending order event asynchronously: orderId={}, userId={}, eventType={}", 
                       orderEvent.getOrderId(), orderEvent.getUserId(), orderEvent.getEventType());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(orderEventsTopic, key, orderEvent);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Order event sent successfully: topic={}, partition={}, offset={}, key={}", 
                               result.getRecordMetadata().topic(),
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset(),
                               key);
                } else {
                    logger.error("Failed to send order event: key={}, error={}", key, exception.getMessage());
                    handleSendFailure(orderEvent, exception);
                }
            });
            
        } catch (Exception e) {
            logger.error("Exception while sending order event: {}", e.getMessage(), e);
            handleSendFailure(orderEvent, e);
        }
    }

    /**
     * Send order event synchronously with timeout
     */
    public boolean sendOrderEventSync(OrderEvent orderEvent, long timeoutSeconds) {
        try {
            String key = orderEvent.getOrderId();
            
            logger.info("Sending order event synchronously: orderId={}, userId={}, eventType={}", 
                       orderEvent.getOrderId(), orderEvent.getUserId(), orderEvent.getEventType());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(orderEventsTopic, key, orderEvent);
            
            SendResult<String, Object> result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            
            logger.info("Order event sent successfully: topic={}, partition={}, offset={}, key={}", 
                       result.getRecordMetadata().topic(),
                       result.getRecordMetadata().partition(),
                       result.getRecordMetadata().offset(),
                       key);
            
            return true;
            
        } catch (TimeoutException e) {
            logger.error("Timeout while sending order event: orderId={}, timeout={}s", 
                        orderEvent.getOrderId(), timeoutSeconds);
            handleSendFailure(orderEvent, e);
            return false;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to send order event synchronously: orderId={}, error={}", 
                        orderEvent.getOrderId(), e.getMessage());
            handleSendFailure(orderEvent, e);
            return false;
        } catch (Exception e) {
            logger.error("Exception while sending order event synchronously: {}", e.getMessage(), e);
            handleSendFailure(orderEvent, e);
            return false;
        }
    }

    /**
     * Send multiple order events in batch
     */
    public void sendOrderEventsBatch(java.util.List<OrderEvent> orderEvents) {
        logger.info("Sending batch of {} order events", orderEvents.size());
        
        for (OrderEvent orderEvent : orderEvents) {
            sendOrderEventAsync(orderEvent);
        }
    }

    /**
     * Send order event partitioned by user ID for user-specific processing
     */
    public void sendOrderEventByUserId(OrderEvent orderEvent) {
        try {
            String partitionKey = orderEvent.getUserId();
            
            logger.info("Sending order event partitioned by userId: orderId={}, userId={}", 
                       orderEvent.getOrderId(), orderEvent.getUserId());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(orderEventsTopic, partitionKey, orderEvent);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Order event partitioned by userId sent: partition={}, offset={}, userId={}", 
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset(),
                               partitionKey);
                } else {
                    logger.error("Failed to send order event partitioned by userId: userId={}, error={}", 
                                partitionKey, exception.getMessage());
                    handleSendFailure(orderEvent, exception);
                }
            });
            
        } catch (Exception e) {
            logger.error("Exception while sending order event partitioned by userId: {}", e.getMessage(), e);
            handleSendFailure(orderEvent, e);
        }
    }

    /**
     * Send high-priority order event (e.g., cancellations, refunds)
     */
    public void sendHighPriorityOrderEvent(OrderEvent orderEvent) {
        try {
            String key = "priority-" + orderEvent.getOrderId();
            
            logger.info("Sending high-priority order event: orderId={}, eventType={}", 
                       orderEvent.getOrderId(), orderEvent.getEventType());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(orderEventsTopic, key, orderEvent);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("High-priority order event sent: partition={}, offset={}, key={}", 
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset(),
                               key);
                } else {
                    logger.error("Failed to send high-priority order event: key={}, error={}", 
                                key, exception.getMessage());
                    handleSendFailure(orderEvent, exception);
                }
            });
            
        } catch (Exception e) {
            logger.error("Exception while sending high-priority order event: {}", e.getMessage(), e);
            handleSendFailure(orderEvent, e);
        }
    }

    /**
     * Handle send failures by logging and optionally sending to error topic
     */
    private void handleSendFailure(OrderEvent orderEvent, Throwable exception) {
        try {
            // Convert order event to JSON for error logging
            String eventJson = objectMapper.writeValueAsString(orderEvent);
            
            // Log the error
            logger.error("Order event send failure - Event: {}, Error: {}", 
                        eventJson, exception.getMessage());
            
            // Optionally send to error topic for manual investigation
            sendToErrorTopic(eventJson, orderEventsTopic, exception.getMessage(), "SEND_FAILURE");
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order event for error handling: {}", e.getMessage());
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
            logger.info("Error event sent to error topic for failed order event");
            
        } catch (Exception e) {
            logger.error("Failed to send error event to error topic: {}", e.getMessage());
        }
    }
}