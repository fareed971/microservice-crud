# State Store Usage in the Microservice Application

## Overview

This microservice application implements a sophisticated state management system using multiple types of state stores to handle event-driven data processing. The architecture combines **persistent state storage**, **stream processing state stores**, and **event-sourcing patterns** to create a robust and scalable system.

## Types of State Stores Used

### 1. MongoDB Persistent State Store

The application uses MongoDB as the primary persistent state store through Spring Data repositories:

#### Repository Layer
- **UserEventRepository** - Stores user events (registrations, logins, logouts)
- **OrderEventRepository** - Stores order-related events (creation, payment, shipping)
- **ProcessedEventRepository** - Stores processed/transformed events
- **ErrorEventRepository** - Stores failed events for error handling

#### Key Features
```java
@Repository
public interface UserEventRepository extends MongoRepository<UserEvent, String> {
    List<UserEvent> findByUserId(String userId);
    List<UserEvent> findByEventType(String eventType);
    List<UserEvent> findByProcessed(boolean processed);
    long countByEventType(String eventType);
}
```

**Benefits:**
- **Durability**: Permanent storage of all events
- **Queryability**: Complex queries for analytics and reporting
- **ACID Properties**: Transactional consistency for critical operations

### 2. Kafka Streams State Stores

The application leverages multiple Kafka Streams state stores for real-time stream processing:

#### A. Materialized KTable for Aggregations
```java
KTable<Windowed<String>, Long> userEventCounts = userEventsByType
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .count(Materialized.as("user-event-counts"));
```

**Purpose:** 
- Real-time counting of user events by type in 5-minute time windows
- Enables immediate access to current aggregated statistics

#### B. Global KTable for Lookups
```java
GlobalKTable<String, UserEvent> userLookupTable = streamsBuilder
    .globalTable(userEventsTopic, 
                Consumed.with(Serdes.String(), userEventSerde),
                Materialized.as("user-lookup-store"));
```

**Purpose:**
- Provides fast user information lookups across all stream processing nodes
- Enables order enrichment with user details in real-time

#### C. Custom Key-Value State Store for Sessions
```java
streamsBuilder.addStateStore(
    Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore("user-session-store"),
        Serdes.String(),
        userEventSerde
    )
);
```

**Purpose:**
- Tracks user session state (login/logout events)
- Calculates session duration
- Maintains user activity state across events

## How State Stores Are Used

### 1. Event Ingestion and Storage Flow

```
User/Order Events → Kafka Topics → Stream Processor → State Updates
                                                   ↓
                              MongoDB ← Event Consumer ← Processed Events
```

#### Process:
1. **Event Creation**: Events are created via REST API (`EventController`)
2. **Kafka Publishing**: Events are sent to Kafka topics (`UserEventProducer`, `OrderEventProducer`)
3. **Stream Processing**: Kafka Streams processes events in real-time (`EventStreamProcessor`)
4. **State Store Updates**: Multiple state stores are updated simultaneously
5. **Persistence**: Consumers save processed events to MongoDB (`UserEventConsumer`, `OrderEventConsumer`)

### 2. Real-Time Processing Patterns

#### Stream Aggregations
- **User Event Counting**: Counts events by type in sliding time windows
- **High-Value Order Detection**: Filters and flags orders above $1000
- **Session Tracking**: Maintains user login/logout state

#### Stream Joins
```java
KStream<String, String> userOrderJoin = userEventsStream
    .selectKey((key, userEvent) -> userEvent.getUserId())
    .join(orderEventsStream.selectKey((key, orderEvent) -> orderEvent.getUserId()),
          (userEvent, orderEvent) -> /* join logic */,
          JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(10)));
```

#### Data Enrichment
```java
KStream<String, ProcessedEvent> enrichedOrders = orderEventsStream
    .leftJoin(userLookupTable,
              (orderKey, order) -> order.getUserId(),
              (order, userEvent) -> /* enrichment logic */);
```

### 3. State Store Access Patterns

#### Read Patterns
- **Query by User ID**: `findByUserId(String userId)`
- **Query by Event Type**: `findByEventType(String eventType)`
- **Time-Range Queries**: `findByTimestampBetween(start, end)`
- **Aggregation Queries**: `countByEventType(String eventType)`

#### Write Patterns
- **Event Persistence**: Direct saves via repositories
- **Batch Operations**: Bulk event processing
- **Stream Updates**: Automatic updates via Kafka Streams

## Benefits of This Architecture

### 1. **Dual Persistence Strategy**
- **Hot Path**: Kafka Streams state stores for real-time processing
- **Cold Path**: MongoDB for long-term persistence and complex queries

### 2. **Scalability**
- Kafka Streams automatically partitions state stores
- MongoDB can be sharded for horizontal scaling
- Independent scaling of producers, consumers, and stream processors

### 3. **Fault Tolerance**
- Kafka Streams automatically backs up state stores to Kafka topics
- MongoDB provides replication and durability
- Event sourcing allows state reconstruction from event logs

### 4. **Real-Time Analytics**
- Immediate access to aggregated metrics via state stores
- Live dashboards can query current state without affecting persistent storage
- Complex event processing with joins and transformations

## State Store Configuration

### Kafka Streams Configuration
```java
// Materialized state stores are configured with:
- Store name for identification
- Key/value serdes for serialization
- Backing topic for fault tolerance
- Caching and changelog configurations
```

### MongoDB Configuration
```java
// Spring Data MongoDB provides:
- Automatic repository implementation
- Query method generation
- Transaction support
- Index management
```

## Monitoring and Observability

The application provides comprehensive monitoring through:
- **Event Counters**: Track processing rates by event type
- **Error Tracking**: Failed events stored in ErrorEventRepository
- **Health Checks**: Kafka connectivity and processing status
- **Metrics Endpoints**: Real-time statistics via REST API

## Use Cases

This state store architecture is ideal for:
- **Event-driven microservices**
- **Real-time analytics and reporting**
- **User behavior tracking**
- **Order processing systems**
- **Fraud detection systems**
- **Session management**
- **Audit logging and compliance**

The combination of persistent and stream-based state stores provides both immediate responsiveness and long-term data durability, making it suitable for enterprise-grade applications requiring both real-time processing and comprehensive data analysis capabilities.