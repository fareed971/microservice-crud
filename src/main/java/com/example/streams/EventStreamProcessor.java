package com.example.streams;

import com.example.model.UserEvent;
import com.example.model.OrderEvent;
import com.example.model.ProcessedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Component
public class EventStreamProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EventStreamProcessor.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;
    
    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;
    
    @Value("${app.kafka.topics.processed-events}")
    private String processedEventsTopic;
    
    @Value("${app.kafka.topics.error-events}")
    private String errorEventsTopic;

    @Bean
    public KStream<String, JsonNode> eventProcessingStream(StreamsBuilder streamsBuilder) {
        
        // Configure serdes
        JsonSerde<JsonNode> jsonSerde = new JsonSerde<>(JsonNode.class, objectMapper);
        JsonSerde<UserEvent> userEventSerde = new JsonSerde<>(UserEvent.class, objectMapper);
        JsonSerde<OrderEvent> orderEventSerde = new JsonSerde<>(OrderEvent.class, objectMapper);
        JsonSerde<ProcessedEvent> processedEventSerde = new JsonSerde<>(ProcessedEvent.class, objectMapper);
        
        // Create user events stream
        KStream<String, UserEvent> userEventsStream = streamsBuilder
            .stream(userEventsTopic, Consumed.with(Serdes.String(), userEventSerde))
            .peek((key, value) -> logger.info("Processing user event in stream: userId={}, eventType={}", 
                                               value.getUserId(), value.getEventType()));
        
        // Create order events stream
        KStream<String, OrderEvent> orderEventsStream = streamsBuilder
            .stream(orderEventsTopic, Consumed.with(Serdes.String(), orderEventSerde))
            .peek((key, value) -> logger.info("Processing order event in stream: orderId={}, eventType={}", 
                                               value.getOrderId(), value.getEventType()));
        
        // Filter high-value orders
        KStream<String, OrderEvent> highValueOrders = orderEventsStream
            .filter((key, order) -> {
                boolean isHighValue = order.getTotalAmount() != null && 
                                    order.getTotalAmount().doubleValue() > 1000.0;
                if (isHighValue) {
                    logger.info("High-value order detected: orderId={}, amount={}", 
                               order.getOrderId(), order.getTotalAmount());
                }
                return isHighValue;
            });
        
        // Send high-value orders to processed events with special marking
        highValueOrders
            .map((key, order) -> {
                ProcessedEvent processedEvent = new ProcessedEvent(
                    order.getId(),
                    "HIGH_VALUE_" + order.getEventType(),
                    "High value order flagged for review",
                    "Amount: " + order.getTotalAmount() + ", Product: " + order.getProductName(),
                    order.getTimestamp()
                );
                return KeyValue.pair(order.getOrderId(), processedEvent);
            })
            .to(processedEventsTopic, Produced.with(Serdes.String(), processedEventSerde));
        
        // Group user events by event type and count them in time windows
        KGroupedStream<String, UserEvent> userEventsByType = userEventsStream
            .groupBy((key, event) -> event.getEventType(), 
                    Grouped.with(Serdes.String(), userEventSerde));
        
        // Count user events by type in 5-minute windows
        KTable<Windowed<String>, Long> userEventCounts = userEventsByType
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("user-event-counts"));
        
        // Output user event counts
        userEventCounts.toStream()
            .peek((windowedKey, count) -> 
                logger.info("User event count: eventType={}, window={}, count={}", 
                           windowedKey.key(), windowedKey.window(), count))
            .map((windowedKey, count) -> {
                ProcessedEvent processedEvent = new ProcessedEvent(
                    null,
                    "USER_EVENT_COUNT",
                    "Event count aggregation",
                    String.format("EventType: %s, Count: %d, Window: %s", 
                                windowedKey.key(), count, windowedKey.window()),
                    LocalDateTime.now()
                );
                return KeyValue.pair(windowedKey.key(), processedEvent);
            })
            .to(processedEventsTopic, Produced.with(Serdes.String(), processedEventSerde));
        
        // Join user events with order events for the same user
        KStream<String, String> userOrderJoin = userEventsStream
            .selectKey((key, userEvent) -> userEvent.getUserId())
            .join(
                orderEventsStream.selectKey((key, orderEvent) -> orderEvent.getUserId()),
                (userEvent, orderEvent) -> {
                    String joinedData = String.format(
                        "User %s performed %s and created order %s for %s", 
                        userEvent.getUserId(),
                        userEvent.getEventType(),
                        orderEvent.getOrderId(),
                        orderEvent.getProductName()
                    );
                    logger.info("User-Order join: {}", joinedData);
                    return joinedData;
                },
                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(10)),
                StreamJoined.with(Serdes.String(), userEventSerde, orderEventSerde)
            );
        
        // Send joined events to processed events topic
        userOrderJoin
            .map((userId, joinedData) -> {
                ProcessedEvent processedEvent = new ProcessedEvent(
                    null,
                    "USER_ORDER_JOIN",
                    "User and order events correlated",
                    joinedData,
                    LocalDateTime.now()
                );
                return KeyValue.pair(userId, processedEvent);
            })
            .to(processedEventsTopic, Produced.with(Serdes.String(), processedEventSerde));
        
        // Create a global KTable for user lookup
        GlobalKTable<String, UserEvent> userLookupTable = streamsBuilder
            .globalTable(userEventsTopic, 
                        Consumed.with(Serdes.String(), userEventSerde),
                        Materialized.as("user-lookup-store"));
        
        // Enrich order events with user information
        KStream<String, ProcessedEvent> enrichedOrders = orderEventsStream
            .leftJoin(
                userLookupTable,
                (orderKey, order) -> order.getUserId(),
                (order, userEvent) -> {
                    String enrichedData = String.format(
                        "Order: %s, Product: %s, Amount: %s, User: %s, Email: %s",
                        order.getOrderId(),
                        order.getProductName(),
                        order.getTotalAmount(),
                        userEvent != null ? userEvent.getUserName() : "Unknown",
                        userEvent != null ? userEvent.getEmail() : "Unknown"
                    );
                    
                    ProcessedEvent processedEvent = new ProcessedEvent(
                        order.getId(),
                        "ENRICHED_" + order.getEventType(),
                        "Order enriched with user data",
                        enrichedData,
                        order.getTimestamp()
                    );
                    
                    logger.info("Order enriched: {}", enrichedData);
                    return processedEvent;
                }
            );
        
        // Send enriched orders to processed events topic
        enrichedOrders.to(processedEventsTopic, Produced.with(Serdes.String(), processedEventSerde));
        
        // Error handling stream - filter potential problematic events
        KStream<String, JsonNode> errorStream = userEventsStream
            .filter((key, event) -> {
                // Example: flag events without proper user ID
                return event.getUserId() == null || event.getUserId().trim().isEmpty();
            })
            .mapValues(event -> {
                try {
                    return objectMapper.valueToTree(event);
                } catch (Exception e) {
                    logger.error("Error converting to JsonNode", e);
                    return objectMapper.createObjectNode();
                }
            });
        
        // Send error events to error topic
        errorStream.to(errorEventsTopic, Produced.with(Serdes.String(), jsonSerde));
        
        // Add state store for maintaining user session information
        streamsBuilder.addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("user-session-store"),
                Serdes.String(),
                userEventSerde
            )
        );
        
        // Process user login/logout events with session tracking
        userEventsStream
            .filter((key, event) -> 
                "USER_LOGIN".equals(event.getEventType()) || "USER_LOGOUT".equals(event.getEventType()))
            .process(() -> new UserSessionProcessor(), "user-session-store");
        
        return userEventsStream.mapValues(event -> {
            try {
                return objectMapper.valueToTree(event);
            } catch (Exception e) {
                logger.error("Error converting UserEvent to JsonNode", e);
                return objectMapper.createObjectNode();
            }
        });
    }
    
    // Custom processor for user session tracking
    private static class UserSessionProcessor implements Processor<String, UserEvent, Void, Void> {
        
        private ProcessorContext<Void, Void> context;
        private org.apache.kafka.streams.state.KeyValueStore<String, UserEvent> sessionStore;
        
        @Override
        public void init(ProcessorContext<Void, Void> context) {
            this.context = context;
            this.sessionStore = context.getStateStore("user-session-store");
        }
        
        @Override
        public void process(Record<String, UserEvent> record) {
            UserEvent event = record.value();
            String userId = event.getUserId();
            
            if ("USER_LOGIN".equals(event.getEventType())) {
                // Store login event
                sessionStore.put(userId, event);
                logger.info("User session started: userId={}", userId);
            } else if ("USER_LOGOUT".equals(event.getEventType())) {
                // Retrieve login event and calculate session duration
                UserEvent loginEvent = sessionStore.get(userId);
                if (loginEvent != null) {
                    Duration sessionDuration = Duration.between(
                        loginEvent.getTimestamp(), 
                        event.getTimestamp()
                    );
                    logger.info("User session ended: userId={}, duration={}ms", 
                               userId, sessionDuration.toMillis());
                    sessionStore.delete(userId);
                }
            }
        }
    }
}