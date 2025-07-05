package com.example.controller;

import com.example.model.UserEvent;
import com.example.model.OrderEvent;
import com.example.model.ProcessedEvent;
import com.example.model.ErrorEvent;
import com.example.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    
    @Autowired
    private EventService eventService;

    // User Event Endpoints
    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> createUserEvent(
            @RequestParam String userId,
            @RequestParam String eventType,
            @RequestParam String userName,
            @RequestParam String email) {
        
        logger.info("Creating user event via API: userId={}, eventType={}", userId, eventType);
        
        try {
            UserEvent userEvent = eventService.createUserEvent(userId, eventType, userName, email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User event created successfully");
            response.put("event", userEvent);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating user event", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create user event: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/user/sync")
    public ResponseEntity<Map<String, Object>> createUserEventSync(
            @RequestParam String userId,
            @RequestParam String eventType,
            @RequestParam String userName,
            @RequestParam String email,
            @RequestParam(defaultValue = "5") long timeoutSeconds) {
        
        logger.info("Creating user event synchronously via API: userId={}, eventType={}", userId, eventType);
        
        try {
            boolean success = eventService.createUserEventSync(userId, eventType, userName, email, timeoutSeconds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "User event created successfully" : "Failed to create user event");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating user event synchronously", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create user event: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserEvent>> getUserEvents(@PathVariable String userId) {
        List<UserEvent> events = eventService.getUserEventsByUserId(userId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/user/type/{eventType}")
    public ResponseEntity<List<UserEvent>> getUserEventsByType(@PathVariable String eventType) {
        List<UserEvent> events = eventService.getUserEventsByType(eventType);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/user/unprocessed")
    public ResponseEntity<List<UserEvent>> getUnprocessedUserEvents() {
        List<UserEvent> events = eventService.getUnprocessedUserEvents();
        return ResponseEntity.ok(events);
    }

    // Order Event Endpoints
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrderEvent(
            @RequestParam String orderId,
            @RequestParam String userId,
            @RequestParam String eventType,
            @RequestParam String productName,
            @RequestParam Integer quantity,
            @RequestParam BigDecimal price) {
        
        logger.info("Creating order event via API: orderId={}, eventType={}", orderId, eventType);
        
        try {
            OrderEvent orderEvent = eventService.createOrderEvent(orderId, userId, eventType, productName, quantity, price);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order event created successfully");
            response.put("event", orderEvent);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating order event", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create order event: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/order/high-priority")
    public ResponseEntity<Map<String, Object>> createHighPriorityOrderEvent(
            @RequestParam String orderId,
            @RequestParam String userId,
            @RequestParam String eventType,
            @RequestParam String productName,
            @RequestParam Integer quantity,
            @RequestParam BigDecimal price) {
        
        logger.info("Creating high-priority order event via API: orderId={}, eventType={}", orderId, eventType);
        
        try {
            eventService.createHighPriorityOrderEvent(orderId, userId, eventType, productName, quantity, price);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "High-priority order event created successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating high-priority order event", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create high-priority order event: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderEvent>> getOrderEvents(@PathVariable String orderId) {
        List<OrderEvent> events = eventService.getOrderEventsByOrderId(orderId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/order/user/{userId}")
    public ResponseEntity<List<OrderEvent>> getOrderEventsByUser(@PathVariable String userId) {
        List<OrderEvent> events = eventService.getOrderEventsByUserId(userId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/order/type/{eventType}")
    public ResponseEntity<List<OrderEvent>> getOrderEventsByType(@PathVariable String eventType) {
        List<OrderEvent> events = eventService.getOrderEventsByType(eventType);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/order/high-value")
    public ResponseEntity<List<OrderEvent>> getHighValueOrders(@RequestParam(defaultValue = "1000") BigDecimal minAmount) {
        List<OrderEvent> events = eventService.getHighValueOrders(minAmount);
        return ResponseEntity.ok(events);
    }

    // Processed Events Endpoints
    @GetMapping("/processed/type/{eventType}")
    public ResponseEntity<List<ProcessedEvent>> getProcessedEventsByType(@PathVariable String eventType) {
        List<ProcessedEvent> events = eventService.getProcessedEventsByType(eventType);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/processed/{originalEventId}")
    public ResponseEntity<ProcessedEvent> getProcessedEventByOriginalId(@PathVariable String originalEventId) {
        Optional<ProcessedEvent> event = eventService.getProcessedEventByOriginalId(originalEventId);
        return event.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Error Events Endpoints
    @GetMapping("/errors/unresolved")
    public ResponseEntity<List<ErrorEvent>> getUnresolvedErrors() {
        List<ErrorEvent> errors = eventService.getUnresolvedErrors();
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/errors/type/{errorType}")
    public ResponseEntity<List<ErrorEvent>> getErrorEventsByType(@PathVariable String errorType) {
        List<ErrorEvent> errors = eventService.getErrorEventsByType(errorType);
        return ResponseEntity.ok(errors);
    }

    @PostMapping("/errors/{errorId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveError(@PathVariable String errorId) {
        try {
            eventService.markErrorAsResolved(errorId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Error marked as resolved");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error resolving error event", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to resolve error: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Test Data Generation Endpoints
    @PostMapping("/test/user-events/{count}")
    public ResponseEntity<Map<String, Object>> generateTestUserEvents(@PathVariable int count) {
        try {
            eventService.generateTestUserEvents(count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", count + " test user events generated");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating test user events", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to generate test events: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/test/order-events/{count}")
    public ResponseEntity<Map<String, Object>> generateTestOrderEvents(@PathVariable int count) {
        try {
            eventService.generateTestOrderEvents(count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", count + " test order events generated");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating test order events", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to generate test events: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/test/high-value-orders/{count}")
    public ResponseEntity<Map<String, Object>> generateTestHighValueOrders(@PathVariable int count) {
        try {
            eventService.generateTestHighValueOrders(count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", count + " test high-value order events generated");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating test high-value order events", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to generate test events: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Statistics and Health Endpoints
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEventStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalEventsProcessed", eventService.getTotalEventsProcessed());
            stats.put("totalEventsInError", eventService.getTotalEventsInError());
            stats.put("kafkaHealthy", eventService.isKafkaHealthy());
            
            // User event statistics
            Map<String, Long> userEventStats = new HashMap<>();
            userEventStats.put("USER_CREATED", eventService.getUserEventCountByType("USER_CREATED"));
            userEventStats.put("USER_LOGIN", eventService.getUserEventCountByType("USER_LOGIN"));
            userEventStats.put("USER_LOGOUT", eventService.getUserEventCountByType("USER_LOGOUT"));
            stats.put("userEventStats", userEventStats);
            
            // Order event statistics
            Map<String, Long> orderEventStats = new HashMap<>();
            orderEventStats.put("ORDER_CREATED", eventService.getOrderEventCountByType("ORDER_CREATED"));
            orderEventStats.put("ORDER_PAID", eventService.getOrderEventCountByType("ORDER_PAID"));
            orderEventStats.put("ORDER_SHIPPED", eventService.getOrderEventCountByType("ORDER_SHIPPED"));
            stats.put("orderEventStats", orderEventStats);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting event stats", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get stats: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean kafkaHealthy = eventService.isKafkaHealthy();
            
            health.put("status", kafkaHealthy ? "UP" : "DOWN");
            health.put("kafka", kafkaHealthy ? "UP" : "DOWN");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Health check failed", e);
            
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            
            return ResponseEntity.status(503).body(health);
        }
    }
}