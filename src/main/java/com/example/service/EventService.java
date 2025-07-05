package com.example.service;

import com.example.model.UserEvent;
import com.example.model.OrderEvent;
import com.example.model.ProcessedEvent;
import com.example.model.ErrorEvent;
import com.example.producer.UserEventProducer;
import com.example.producer.OrderEventProducer;
import com.example.repository.UserEventRepository;
import com.example.repository.OrderEventRepository;
import com.example.repository.ProcessedEventRepository;
import com.example.repository.ErrorEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    
    @Autowired
    private UserEventProducer userEventProducer;
    
    @Autowired
    private OrderEventProducer orderEventProducer;
    
    @Autowired
    private UserEventRepository userEventRepository;
    
    @Autowired
    private OrderEventRepository orderEventRepository;
    
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    
    @Autowired
    private ErrorEventRepository errorEventRepository;

    // User Event Operations
    public UserEvent createUserEvent(String userId, String eventType, String userName, String email) {
        logger.info("Creating user event: userId={}, eventType={}", userId, eventType);
        
        UserEvent userEvent = new UserEvent(userId, eventType, userName, email);
        
        // Send to Kafka asynchronously
        userEventProducer.sendUserEventAsync(userEvent);
        
        return userEvent;
    }

    public boolean createUserEventSync(String userId, String eventType, String userName, String email, long timeoutSeconds) {
        logger.info("Creating user event synchronously: userId={}, eventType={}", userId, eventType);
        
        UserEvent userEvent = new UserEvent(userId, eventType, userName, email);
        
        // Send to Kafka synchronously
        return userEventProducer.sendUserEventSync(userEvent, timeoutSeconds);
    }

    public void createUserEventsBatch(List<UserEvent> userEvents) {
        logger.info("Creating batch of {} user events", userEvents.size());
        userEventProducer.sendUserEventsBatch(userEvents);
    }

    // Order Event Operations
    public OrderEvent createOrderEvent(String orderId, String userId, String eventType, 
                                     String productName, Integer quantity, BigDecimal price) {
        logger.info("Creating order event: orderId={}, userId={}, eventType={}", orderId, userId, eventType);
        
        OrderEvent orderEvent = new OrderEvent(orderId, userId, eventType, productName, quantity, price);
        
        // Send to Kafka asynchronously
        orderEventProducer.sendOrderEventAsync(orderEvent);
        
        return orderEvent;
    }

    public boolean createOrderEventSync(String orderId, String userId, String eventType, 
                                      String productName, Integer quantity, BigDecimal price, long timeoutSeconds) {
        logger.info("Creating order event synchronously: orderId={}, userId={}, eventType={}", orderId, userId, eventType);
        
        OrderEvent orderEvent = new OrderEvent(orderId, userId, eventType, productName, quantity, price);
        
        // Send to Kafka synchronously
        return orderEventProducer.sendOrderEventSync(orderEvent, timeoutSeconds);
    }

    public void createOrderEventsBatch(List<OrderEvent> orderEvents) {
        logger.info("Creating batch of {} order events", orderEvents.size());
        orderEventProducer.sendOrderEventsBatch(orderEvents);
    }

    public void createHighPriorityOrderEvent(String orderId, String userId, String eventType, 
                                           String productName, Integer quantity, BigDecimal price) {
        logger.info("Creating high-priority order event: orderId={}, eventType={}", orderId, eventType);
        
        OrderEvent orderEvent = new OrderEvent(orderId, userId, eventType, productName, quantity, price);
        orderEventProducer.sendHighPriorityOrderEvent(orderEvent);
    }

    // Query Operations
    public List<UserEvent> getUserEventsByUserId(String userId) {
        return userEventRepository.findByUserId(userId);
    }

    public List<UserEvent> getUserEventsByType(String eventType) {
        return userEventRepository.findByEventType(eventType);
    }

    public List<UserEvent> getUnprocessedUserEvents() {
        return userEventRepository.findByProcessed(false);
    }

    public List<OrderEvent> getOrderEventsByOrderId(String orderId) {
        return orderEventRepository.findByOrderId(orderId);
    }

    public List<OrderEvent> getOrderEventsByUserId(String userId) {
        return orderEventRepository.findByUserId(userId);
    }

    public List<OrderEvent> getOrderEventsByType(String eventType) {
        return orderEventRepository.findByEventType(eventType);
    }

    public List<OrderEvent> getUnprocessedOrderEvents() {
        return orderEventRepository.findByProcessed(false);
    }

    public List<OrderEvent> getHighValueOrders(BigDecimal minAmount) {
        return orderEventRepository.findByTotalAmountGreaterThanEqual(minAmount);
    }

    public List<ProcessedEvent> getProcessedEventsByType(String eventType) {
        return processedEventRepository.findByEventType(eventType);
    }

    public Optional<ProcessedEvent> getProcessedEventByOriginalId(String originalEventId) {
        return processedEventRepository.findByOriginalEventId(originalEventId);
    }

    public List<ErrorEvent> getUnresolvedErrors() {
        return errorEventRepository.findByResolved(false);
    }

    public List<ErrorEvent> getErrorEventsByType(String errorType) {
        return errorEventRepository.findByErrorType(errorType);
    }

    public List<ErrorEvent> getRetryableErrors(Integer maxRetries) {
        return errorEventRepository.findRetryableErrors(maxRetries);
    }

    // Statistics and Analytics
    public long getUserEventCountByType(String eventType) {
        return userEventRepository.countByEventType(eventType);
    }

    public long getOrderEventCountByType(String eventType) {
        return orderEventRepository.countByEventType(eventType);
    }

    public long getProcessedEventCountByType(String eventType) {
        return processedEventRepository.countByEventType(eventType);
    }

    public long getErrorEventCountByType(String errorType) {
        return errorEventRepository.countByErrorType(errorType);
    }

    // Test Data Generation
    public void generateTestUserEvents(int count) {
        logger.info("Generating {} test user events", count);
        
        String[] eventTypes = {"USER_CREATED", "USER_UPDATED", "USER_LOGIN", "USER_LOGOUT", "USER_DELETED"};
        String[] userNames = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry"};
        
        for (int i = 0; i < count; i++) {
            String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
            String eventType = eventTypes[i % eventTypes.length];
            String userName = userNames[i % userNames.length];
            String email = userName.toLowerCase() + "@example.com";
            
            createUserEvent(userId, eventType, userName, email);
            
            // Add small delay to avoid overwhelming the system
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void generateTestOrderEvents(int count) {
        logger.info("Generating {} test order events", count);
        
        String[] eventTypes = {"ORDER_CREATED", "ORDER_PAID", "ORDER_SHIPPED", "ORDER_DELIVERED", "ORDER_CANCELLED"};
        String[] products = {"Laptop", "Phone", "Tablet", "Headphones", "Camera", "Watch", "Book", "Chair"};
        
        for (int i = 0; i < count; i++) {
            String orderId = "order-" + UUID.randomUUID().toString().substring(0, 8);
            String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
            String eventType = eventTypes[i % eventTypes.length];
            String productName = products[i % products.length];
            Integer quantity = (i % 5) + 1;
            BigDecimal price = BigDecimal.valueOf(100 + (i % 1900)); // Price between 100 and 2000
            
            createOrderEvent(orderId, userId, eventType, productName, quantity, price);
            
            // Add small delay to avoid overwhelming the system
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void generateTestHighValueOrders(int count) {
        logger.info("Generating {} test high-value order events", count);
        
        String[] eventTypes = {"ORDER_CREATED", "ORDER_PAID"};
        String[] products = {"Luxury Watch", "Diamond Ring", "High-End Laptop", "Premium Camera", "Designer Handbag"};
        
        for (int i = 0; i < count; i++) {
            String orderId = "high-value-order-" + UUID.randomUUID().toString().substring(0, 8);
            String userId = "premium-user-" + UUID.randomUUID().toString().substring(0, 8);
            String eventType = eventTypes[i % eventTypes.length];
            String productName = products[i % products.length];
            Integer quantity = 1;
            BigDecimal price = BigDecimal.valueOf(1500 + (i % 3500)); // Price between 1500 and 5000
            
            createOrderEvent(orderId, userId, eventType, productName, quantity, price);
            
            // Add small delay
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Error Resolution
    public void markErrorAsResolved(String errorId) {
        Optional<ErrorEvent> errorEventOpt = errorEventRepository.findById(errorId);
        if (errorEventOpt.isPresent()) {
            ErrorEvent errorEvent = errorEventOpt.get();
            errorEvent.setResolved(true);
            errorEventRepository.save(errorEvent);
            logger.info("Error marked as resolved: errorId={}", errorId);
        } else {
            logger.warn("Error event not found: errorId={}", errorId);
        }
    }

    // Health Check Methods
    public boolean isKafkaHealthy() {
        try {
            // Simple health check by creating a test event
            UserEvent testEvent = new UserEvent("health-check-user", "HEALTH_CHECK", "Health Check", "health@example.com");
            return userEventProducer.sendUserEventSync(testEvent, 5);
        } catch (Exception e) {
            logger.error("Kafka health check failed", e);
            return false;
        }
    }

    public long getTotalEventsProcessed() {
        return userEventRepository.countByProcessed(true) + orderEventRepository.countByProcessed(true);
    }

    public long getTotalEventsInError() {
        return errorEventRepository.countByResolved(false);
    }
}