package com.example.streams;

import com.example.model.UserEvent;
import com.example.model.OrderEvent;
import com.example.model.ProcessedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

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
            .mapValues(order -> {
                ProcessedEvent processedEvent = new ProcessedEvent(
                    order.getId(),
                    "HIGH_VALUE_" + order.getEventType(),
                    "High value order flagged for review",
                    "Amount: " + order.getTotalAmount() + ", Product: " + order.getProductName(),
                    order.getTimestamp()
                );
                return processedEvent;
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
                    "EventType: " + windowedKey.key() + ", Count: " + count + ", Window: " + windowedKey.window(),
                    LocalDateTime.now()
                );
                return KeyValue.pair(windowedKey.key(), processedEvent);
            })
            .to(processedEventsTopic, Produced.with(Serdes.String(), processedEventSerde));
        
        // Simple join between user events and order events
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
            .mapValues(joinedData -> {
                ProcessedEvent processedEvent = new ProcessedEvent(
                    null,
                    "USER_ORDER_JOIN",
                    "User and order events correlated",
                    joinedData,
                    LocalDateTime.now()
                );
                return processedEvent;
            })
            .to(processedEventsTopic, Produced.with(Serdes.String(), processedEventSerde));
        
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
        errorStream.to(errorEventsTopic);
        
        // Return the main stream as JsonNode
        return userEventsStream.mapValues(event -> {
            try {
                return objectMapper.valueToTree(event);
            } catch (Exception e) {
                logger.error("Error converting UserEvent to JsonNode", e);
                return objectMapper.createObjectNode();
            }
        });
    }
}