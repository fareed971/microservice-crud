package com.example.consumer;

import com.example.model.OrderEvent;
import com.example.model.ErrorEvent;
import com.example.repository.OrderEventRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);
    
    @Autowired
    private OrderEventRepository orderEventRepository;
    
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
        topics = "${app.kafka.topics.order-events}",
        groupId = "order-events-group",
        containerFactory = "orderEventsKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeOrderEvent(
            @Payload OrderEvent orderEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Consuming order event: orderId={}, userId={}, eventType={}, topic={}, partition={}, offset={}", 
                       orderEvent.getOrderId(), orderEvent.getUserId(), orderEvent.getEventType(), 
                       topic, partition, offset);
            
            // Process the order event
            processOrderEvent(orderEvent);
            
            // Save to MongoDB
            OrderEvent savedEvent = orderEventRepository.save(orderEvent);
            logger.info("Order event saved to MongoDB: id={}", savedEvent.getId());
            
            // Send processed event to processed events topic
            sendProcessedEvent(orderEvent, "ORDER_EVENT_PROCESSED");
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            logger.info("Order event processed successfully: orderId={}", orderEvent.getOrderId());
            
        } catch (Exception e) {
            logger.error("Error processing order event: orderId={}, error={}", 
                        orderEvent.getOrderId(), e.getMessage(), e);
            
            // Handle the error
            handleProcessingError(orderEvent, e, topic, "ORDER_EVENT_CONSUMER");
            
            // Don't acknowledge - let retry mechanism handle it
            throw new RuntimeException("Failed to process order event", e);
        }
    }

    @KafkaListener(
        topics = "${app.kafka.topics.order-events}.DLT",
        groupId = "order-events-dlt-group"
    )
    public void consumeOrderEventDLT(
            @Payload OrderEvent orderEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            Acknowledgment acknowledgment) {
        
        logger.error("Processing order event from DLT: orderId={}, topic={}, error={}", 
                    orderEvent.getOrderId(), topic, exceptionMessage);
        
        try {
            // Save to error collection in MongoDB
            ErrorEvent errorEvent = new ErrorEvent(
                objectMapper.writeValueAsString(orderEvent),
                topic.replace(".DLT", ""),
                exceptionMessage,
                "DLT_PROCESSING",
                "CONSUMER_DLT"
            );
            errorEvent.setRetryCount(3); // Max retries exceeded
            
            errorEventRepository.save(errorEvent);
            logger.info("Order event error saved to MongoDB from DLT");
            
            // Send to dead letter topic for manual investigation
            kafkaTemplate.send(deadLetterTopic, orderEvent.getOrderId(), orderEvent);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing order event from DLT: {}", e.getMessage(), e);
        }
    }

    private void processOrderEvent(OrderEvent orderEvent) {
        // Business logic for processing order events
        switch (orderEvent.getEventType().toUpperCase()) {
            case "ORDER_CREATED":
                processOrderCreated(orderEvent);
                break;
            case "ORDER_UPDATED":
                processOrderUpdated(orderEvent);
                break;
            case "ORDER_PAID":
                processOrderPaid(orderEvent);
                break;
            case "ORDER_SHIPPED":
                processOrderShipped(orderEvent);
                break;
            case "ORDER_DELIVERED":
                processOrderDelivered(orderEvent);
                break;
            case "ORDER_CANCELLED":
                processOrderCancelled(orderEvent);
                break;
            case "ORDER_REFUNDED":
                processOrderRefunded(orderEvent);
                break;
            default:
                logger.warn("Unknown order event type: {}", orderEvent.getEventType());
                orderEvent.setStatus("UNKNOWN_EVENT_PROCESSED");
        }
        
        // Mark as processed
        orderEvent.setProcessed(true);
        orderEvent.setTimestamp(LocalDateTime.now());
    }

    private void processOrderCreated(OrderEvent orderEvent) {
        logger.info("Processing order created event: orderId={}, amount={}", 
                   orderEvent.getOrderId(), orderEvent.getTotalAmount());
        
        orderEvent.setStatus("CREATED");
        
        // Business logic for order creation
        validateOrderAmount(orderEvent);
        
        // Here you could:
        // - Validate inventory
        // - Calculate taxes
        // - Apply discounts
        // - Reserve stock
        // - Send confirmation email
    }

    private void processOrderUpdated(OrderEvent orderEvent) {
        logger.info("Processing order updated event: orderId={}", orderEvent.getOrderId());
        
        orderEvent.setStatus("UPDATED");
        
        // Here you could:
        // - Update inventory reservations
        // - Recalculate totals
        // - Update delivery estimates
        // - Notify customer of changes
    }

    private void processOrderPaid(OrderEvent orderEvent) {
        logger.info("Processing order paid event: orderId={}, amount={}", 
                   orderEvent.getOrderId(), orderEvent.getTotalAmount());
        
        orderEvent.setStatus("PAID");
        
        // Here you could:
        // - Confirm payment
        // - Update accounting
        // - Initiate fulfillment
        // - Send payment confirmation
        // - Update loyalty points
    }

    private void processOrderShipped(OrderEvent orderEvent) {
        logger.info("Processing order shipped event: orderId={}", orderEvent.getOrderId());
        
        orderEvent.setStatus("SHIPPED");
        
        // Here you could:
        // - Update tracking information
        // - Send shipping notification
        // - Update delivery estimates
        // - Release inventory
    }

    private void processOrderDelivered(OrderEvent orderEvent) {
        logger.info("Processing order delivered event: orderId={}", orderEvent.getOrderId());
        
        orderEvent.setStatus("DELIVERED");
        
        // Here you could:
        // - Complete the order
        // - Request review
        // - Update customer satisfaction metrics
        // - Process any pending refunds
    }

    private void processOrderCancelled(OrderEvent orderEvent) {
        logger.info("Processing order cancelled event: orderId={}", orderEvent.getOrderId());
        
        orderEvent.setStatus("CANCELLED");
        
        // Here you could:
        // - Release inventory
        // - Process refunds
        // - Update analytics
        // - Send cancellation confirmation
    }

    private void processOrderRefunded(OrderEvent orderEvent) {
        logger.info("Processing order refunded event: orderId={}, amount={}", 
                   orderEvent.getOrderId(), orderEvent.getTotalAmount());
        
        orderEvent.setStatus("REFUNDED");
        
        // Here you could:
        // - Process refund payment
        // - Update accounting
        // - Update inventory
        // - Send refund confirmation
    }

    private void validateOrderAmount(OrderEvent orderEvent) {
        // Validate order amount
        if (orderEvent.getTotalAmount() == null || 
            orderEvent.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid order amount: " + orderEvent.getTotalAmount());
        }
        
        // Check for suspiciously high amounts
        if (orderEvent.getTotalAmount().compareTo(new BigDecimal("10000")) > 0) {
            logger.warn("High value order detected: orderId={}, amount={}", 
                       orderEvent.getOrderId(), orderEvent.getTotalAmount());
            // Could trigger manual review or additional verification
        }
    }

    private void sendProcessedEvent(OrderEvent orderEvent, String processingResult) {
        try {
            com.example.model.ProcessedEvent processedEvent = new com.example.model.ProcessedEvent(
                orderEvent.getId(),
                orderEvent.getEventType(),
                processingResult,
                "Order Status: " + orderEvent.getStatus() + ", Amount: " + orderEvent.getTotalAmount(),
                orderEvent.getTimestamp()
            );
            
            kafkaTemplate.send(processedEventsTopic, orderEvent.getOrderId(), processedEvent);
            logger.info("Processed event sent: orderId={}, result={}", orderEvent.getOrderId(), processingResult);
            
        } catch (Exception e) {
            logger.error("Failed to send processed event: orderId={}, error={}", 
                        orderEvent.getOrderId(), e.getMessage());
        }
    }

    private void handleProcessingError(OrderEvent orderEvent, Exception exception, 
                                     String originalTopic, String processingStage) {
        try {
            String eventJson = objectMapper.writeValueAsString(orderEvent);
            
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
            
            logger.info("Error event created and sent for order event: orderId={}", orderEvent.getOrderId());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order event for error handling: {}", e.getMessage());
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