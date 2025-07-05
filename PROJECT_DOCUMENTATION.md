# 📚 Kafka Streams MongoDB Application - Complete Documentation

## 🏗️ Project Architecture Overview

This application demonstrates a complete **event-driven microservices architecture** using **Apache Kafka Streams** for real-time data processing and **MongoDB** for data persistence.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST API      │    │   Kafka Broker  │    │   MongoDB       │
│   (Spring Boot) │    │   (Topics)      │    │   (Collections) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Producers     │───▶│   Kafka Topics  │    │   Data Storage  │
│   - UserEvent   │    │   - user-events │    │   - userEvents  │
│   - OrderEvent  │    │   - order-events│    │   - orderEvents │
└─────────────────┘    │   - processed   │    │   - processed   │
         │              │   - errors      │    │   - errors      │
         ▼              │   - dead-letter │    └─────────────────┘
┌─────────────────┐    └─────────────────┘
│   Consumers     │              │
│   - UserEvent   │◀─────────────┘
│   - OrderEvent  │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│ Stream Processor│
│ - Aggregations  │
│ - Joins         │
│ - Transformations│
└─────────────────┘
```

## 🔄 Data Flow Architecture

### **Complete Data Flow**

```
1. REST API Request
   ↓
2. Producer (Async/Sync)
   ↓
3. Kafka Topic
   ↓
4. Stream Processor (Real-time Processing)
   ↓
5. Consumer (Message Processing)
   ↓
6. MongoDB Storage
   ↓
7. Processed Events Topic
```

## 📊 Kafka Topics Structure

### **Primary Topics**

| Topic Name | Purpose | Partitions | Replication | Producer | Consumer |
|------------|---------|------------|-------------|----------|----------|
| `user-events` | User activities (login, logout, creation) | 1 | 1 | UserEventProducer | UserEventConsumer |
| `order-events` | Order lifecycle events | 1 | 1 | OrderEventProducer | OrderEventConsumer |
| `processed-events` | Successfully processed events | 1 | 1 | Stream Processor | - |
| `error-events` | Failed events and errors | 1 | 1 | Error Handler | - |
| `dead-letter-topic` | Failed messages after retries | 1 | 1 | Consumer Error Handler | - |

### **Topic Creation**

Topics are **automatically created** when:
- First message is produced
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'` is set in docker-compose

## 🏭 Producers Deep Dive

### **UserEventProducer (`src/main/java/com/example/producer/UserEventProducer.java`)**

#### **Purpose**
Publishes user-related events to the `user-events` topic.

#### **Event Types**
- `USER_CREATED` - New user registration
- `USER_LOGIN` - User login activity
- `USER_LOGOUT` - User logout activity
- `USER_UPDATED` - User profile updates

#### **Key Methods**

```java
// Asynchronous publishing
public CompletableFuture<SendResult<String, UserEvent>> publishUserEventAsync(UserEvent event)

// Synchronous publishing
public boolean publishUserEventSync(UserEvent event, long timeoutSeconds)

// Batch publishing
public void publishUserEventsBatch(List<UserEvent> events)
```

#### **Configuration**
```yaml
spring:
  kafka:
    producer:
      key-serializer: StringSerializer
      value-serializer: JsonSerializer
      acks: all              # Wait for all replicas
      retries: 3             # Retry failed sends
      batch-size: 16384      # Batch size for efficiency
```

#### **Error Handling**
- **Retry Logic**: 3 attempts with exponential backoff
- **Dead Letter Queue**: Failed messages sent to `dead-letter-topic`
- **Monitoring**: Metrics and logging for failed sends

### **OrderEventProducer (`src/main/java/com/example/producer/OrderEventProducer.java`)**

#### **Purpose**
Publishes order-related events to the `order-events` topic.

#### **Event Types**
- `ORDER_CREATED` - New order placed
- `ORDER_PAID` - Payment processed
- `ORDER_SHIPPED` - Order shipped
- `ORDER_DELIVERED` - Order delivered
- `ORDER_CANCELLED` - Order cancelled

#### **Key Features**
- **Custom Partitioning**: Orders distributed by user ID
- **High-Priority Queue**: Express orders get priority
- **Idempotency**: Duplicate order prevention
- **Transactional Support**: Ensures exactly-once delivery

## 🛠️ Consumers Deep Dive

### **UserEventConsumer (`src/main/java/com/example/consumer/UserEventConsumer.java`)**

#### **Purpose**
Processes user events from `user-events` topic and stores them in MongoDB.

#### **Processing Logic**

```java
@KafkaListener(topics = "user-events", groupId = "kafka-streams-group")
public void consumeUserEvent(UserEvent event) {
    try {
        // 1. Validate event
        validateUserEvent(event);
        
        // 2. Business logic processing
        processUserEvent(event);
        
        // 3. Store in MongoDB
        userEventRepository.save(event);
        
        // 4. Manual acknowledgment
        acknowledgment.acknowledge();
        
    } catch (Exception e) {
        // Error handling and retry logic
        handleError(event, e);
    }
}
```

#### **Error Handling**
- **Retry Mechanism**: 3 attempts with exponential backoff
- **Dead Letter Topic**: Failed messages after max retries
- **Error Tracking**: Error events stored in MongoDB
- **Circuit Breaker**: Prevents cascading failures

### **OrderEventConsumer (`src/main/java/com/example/consumer/OrderEventConsumer.java`)**

#### **Purpose**
Processes order events and implements complex business logic.

#### **Processing Features**
- **High-Value Order Detection**: Orders > $1000 flagged for review
- **Inventory Management**: Stock level updates
- **Payment Processing**: Payment validation and processing
- **Shipping Orchestration**: Shipping provider integration

## 🌊 Event Stream Processor Deep Dive

### **EventStreamProcessor (`src/main/java/com/example/streams/EventStreamProcessor.java`)**

#### **Purpose**
Real-time stream processing using Kafka Streams for advanced analytics and event correlation.

#### **Processing Topology**

```
User Events Stream ──┐
                    ├─▶ Stream-Stream Join ──▶ Enriched Events
Order Events Stream ─┘

User Events ──▶ Time Window (5 min) ──▶ Aggregation ──▶ Event Counts

Order Events ──▶ Filter (High Value) ──▶ High-Value Orders Topic

All Events ──▶ Error Detection ──▶ Error Events Topic
```

#### **Key Processing Features**

##### **1. High-Value Order Detection**
```java
KStream<String, OrderEvent> highValueOrders = orderEventsStream
    .filter((key, order) -> order.getTotalAmount().compareTo(new BigDecimal("1000")) > 0)
    .peek((key, order) -> logger.info("High-value order detected: {}", order.getOrderId()));
```

##### **2. Time-Windowed Aggregations**
```java
KTable<Windowed<String>, Long> userEventCounts = userEventsStream
    .groupBy((key, event) -> event.getEventType())
    .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
    .count(Materialized.as("user-event-counts"));
```

##### **3. Stream-Stream Joins**
```java
KStream<String, String> userOrderJoin = userEventsStream
    .join(orderEventsStream,
          (userEvent, orderEvent) -> "User: " + userEvent.getUserName() + 
                                   " placed order: " + orderEvent.getOrderId(),
          JoinWindows.of(Duration.ofMinutes(10)));
```

##### **4. Global Table Joins**
```java
KStream<String, ProcessedEvent> enrichedOrders = orderEventsStream
    .join(userEventsGlobalTable,
          (orderId, order) -> order.getUserId(),
          (order, userEvent) -> enrichOrderWithUserInfo(order, userEvent));
```

#### **State Stores**
- **user-session-store**: Track user login/logout sessions
- **order-aggregation-store**: Order statistics and metrics
- **user-event-counts**: Real-time event counting

## 🗄️ MongoDB Collections

### **Data Models and Collections**

#### **1. userEvents Collection**
```javascript
{
  "_id": ObjectId("..."),
  "userId": "user123",
  "eventType": "USER_LOGIN",
  "userName": "John Doe",
  "email": "john@example.com",
  "timestamp": ISODate("2024-01-01T12:00:00Z"),
  "processed": false,
  "metadata": {
    "source": "web",
    "sessionId": "session123"
  }
}
```

#### **2. orderEvents Collection**
```javascript
{
  "_id": ObjectId("..."),
  "orderId": "order456",
  "userId": "user123",
  "eventType": "ORDER_CREATED",
  "productName": "Laptop",
  "quantity": 1,
  "price": 999.99,
  "totalAmount": 999.99,
  "timestamp": ISODate("2024-01-01T12:00:00Z"),
  "processed": false,
  "highValue": false
}
```

#### **3. processedEvents Collection**
```javascript
{
  "_id": ObjectId("..."),
  "eventId": "event789",
  "eventType": "USER_ORDER_JOIN",
  "description": "User and order events correlated",
  "details": "User John Doe placed order order456",
  "timestamp": ISODate("2024-01-01T12:00:00Z"),
  "processingTime": 150,
  "sourceEvents": ["user123", "order456"]
}
```

#### **4. errorEvents Collection**
```javascript
{
  "_id": ObjectId("..."),
  "eventId": "error123",
  "eventType": "PROCESSING_ERROR",
  "errorMessage": "Invalid order format",
  "originalEvent": { ... },
  "retryCount": 2,
  "resolved": false,
  "timestamp": ISODate("2024-01-01T12:00:00Z"),
  "stackTrace": "..."
}
```

## 🔌 REST API Endpoints

### **User Event Endpoints**

#### **Create User Event**
```bash
# Async creation
POST /api/events/user
Content-Type: application/x-www-form-urlencoded

userId=user123&eventType=USER_LOGIN&userName=John Doe&email=john@example.com

# Sync creation with timeout
POST /api/events/user/sync
Content-Type: application/x-www-form-urlencoded

userId=user123&eventType=USER_LOGIN&userName=John Doe&email=john@example.com&timeoutSeconds=10
```

#### **Query User Events**
```bash
# Get events by user ID
GET /api/events/user/user123

# Get events by type
GET /api/events/user/type/USER_LOGIN

# Get unprocessed events
GET /api/events/user/unprocessed
```

### **Order Event Endpoints**

#### **Create Order Event**
```bash
# Standard order
POST /api/events/order
Content-Type: application/x-www-form-urlencoded

orderId=order456&userId=user123&eventType=ORDER_CREATED&productName=Laptop&quantity=1&price=999.99

# High-priority order
POST /api/events/order/high-priority
Content-Type: application/x-www-form-urlencoded

orderId=order789&userId=user123&eventType=ORDER_CREATED&productName=Server&quantity=2&price=5000.00
```

#### **Query Order Events**
```bash
# Get orders by order ID
GET /api/events/order/order456

# Get orders by user
GET /api/events/order/user/user123

# Get high-value orders
GET /api/events/order/high-value?minAmount=1000
```

### **Test Data Generation**

```bash
# Generate test user events
POST /api/events/test/user-events/10

# Generate test order events
POST /api/events/test/order-events/5

# Generate high-value orders
POST /api/events/test/high-value-orders/3
```

### **Monitoring and Health**

```bash
# Application health
GET /api/events/health

# Application statistics
GET /api/events/stats

# Error management
GET /api/events/errors/unresolved
POST /api/events/errors/{errorId}/resolve
```

## 🚀 Getting Started Guide

### **1. Prerequisites**
```bash
# Required software
- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- MongoDB (or use Docker)
```

### **2. Environment Setup**
```bash
# Clone the repository
git clone <repository-url>
cd microservice-crud

# Start infrastructure
docker-compose up -d zookeeper kafka kafka-ui

# Wait for Kafka to be ready (30-60 seconds)
docker logs kafka | grep "started"
```

### **3. Application Startup**
```bash
# Build and run
mvn clean install
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring.profiles.active=dev
```

### **4. Verify Installation**
```bash
# Check health
curl http://localhost:8080/api/events/health

# Generate test data
curl -X POST http://localhost:8080/api/events/test/user-events/5

# Verify data creation
curl http://localhost:8080/api/events/user/unprocessed
```

## 📊 Monitoring and Observability

### **Kafka UI**
- **URL**: `http://localhost:8081`
- **Features**: Topic management, message browsing, consumer groups

### **MongoDB Express** (Optional)
- **URL**: `http://localhost:8082`
- **Credentials**: admin/admin123
- **Features**: Database browsing, collection management

### **Application Metrics**
```bash
# Real-time statistics
curl http://localhost:8080/api/events/stats

# Sample response
{
  "totalEventsProcessed": 1250,
  "totalEventsInError": 3,
  "kafkaHealthy": true,
  "userEventStats": {
    "USER_LOGIN": 450,
    "USER_LOGOUT": 400,
    "USER_CREATED": 250
  },
  "orderEventStats": {
    "ORDER_CREATED": 300,
    "ORDER_PAID": 280,
    "ORDER_SHIPPED": 250
  }
}
```

## 🔧 Configuration

### **Application Configuration (`application.yml`)**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      retries: 3
      batch-size: 16384
    consumer:
      group-id: kafka-streams-group
      auto-offset-reset: earliest
    streams:
      application-id: kafka-streams-app
      processing-guarantee: exactly_once_v2
  
  data:
    mongodb:
      host: localhost
      port: 27017
      database: kafka_streams_db
```

### **Topic Configuration**
```yaml
app:
  kafka:
    topics:
      user-events: user-events
      order-events: order-events
      processed-events: processed-events
      error-events: error-events
      dead-letter: dead-letter-topic
```

## 🧪 Testing Scenarios

### **1. Basic Event Flow**
```bash
# Create user event
curl -X POST "http://localhost:8080/api/events/user" \
  -d "userId=test1&eventType=USER_LOGIN&userName=Test User&email=test@example.com"

# Create order event
curl -X POST "http://localhost:8080/api/events/order" \
  -d "orderId=order1&userId=test1&eventType=ORDER_CREATED&productName=Laptop&quantity=1&price=999.99"

# Check processed events
curl "http://localhost:8080/api/events/processed/type/USER_ORDER_JOIN"
```

### **2. High-Value Order Processing**
```bash
# Create high-value order
curl -X POST "http://localhost:8080/api/events/order" \
  -d "orderId=order2&userId=test1&eventType=ORDER_CREATED&productName=Server&quantity=1&price=5000.00"

# Check high-value orders
curl "http://localhost:8080/api/events/order/high-value"
```

### **3. Error Handling**
```bash
# Create invalid event (will trigger error handling)
curl -X POST "http://localhost:8080/api/events/user" \
  -d "userId=&eventType=INVALID&userName=&email=invalid-email"

# Check error events
curl "http://localhost:8080/api/events/errors/unresolved"
```

## 📈 Performance Optimization

### **Producer Optimization**
- **Batch Processing**: Reduce network calls
- **Compression**: Use `snappy` or `gzip`
- **Asynchronous Sends**: Non-blocking operations

### **Consumer Optimization**
- **Parallel Processing**: Multiple consumer instances
- **Batch Processing**: Process messages in batches
- **Offset Management**: Manual acknowledgment

### **Stream Processing**
- **State Store Tuning**: Optimize state store configuration
- **Windowing**: Appropriate window sizes
- **Parallelism**: Multiple stream threads

## 🚨 Troubleshooting

### **Common Issues**

#### **1. Kafka Connection Failed**
```bash
# Check Kafka status
docker ps | grep kafka
docker logs kafka

# Restart Kafka
docker-compose restart kafka
```

#### **2. MongoDB Connection Failed**
```bash
# Check MongoDB status
mongo --eval "db.runCommand('ping')"

# Check connection string in application.yml
```

#### **3. Consumer Lag**
```bash
# Check consumer group status
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group kafka-streams-group --describe
```

#### **4. Stream Processing Issues**
```bash
# Check stream processor logs
grep "EventStreamProcessor" logs/application.log

# Verify state stores
curl http://localhost:8080/actuator/metrics/kafka.stream.state.store.size
```

## 🎯 Best Practices

### **1. Event Design**
- **Immutable Events**: Never modify published events
- **Schema Evolution**: Plan for backward compatibility
- **Event Sourcing**: Events as single source of truth

### **2. Error Handling**
- **Graceful Degradation**: Fail safely
- **Retry Logic**: Exponential backoff
- **Dead Letter Queues**: Handle poison messages

### **3. Monitoring**
- **Health Checks**: Regular endpoint monitoring
- **Metrics**: Track throughput, latency, errors
- **Alerting**: Set up proactive alerts

### **4. Security**
- **Authentication**: Secure API endpoints
- **Authorization**: Role-based access control
- **Encryption**: SSL/TLS for data in transit

## 📝 API Reference

### **Complete Endpoint List**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events/health` | Application health check |
| GET | `/api/events/stats` | Application statistics |
| POST | `/api/events/user` | Create user event |
| POST | `/api/events/user/sync` | Create user event (sync) |
| GET | `/api/events/user/{userId}` | Get user events |
| GET | `/api/events/user/type/{eventType}` | Get user events by type |
| GET | `/api/events/user/unprocessed` | Get unprocessed user events |
| POST | `/api/events/order` | Create order event |
| POST | `/api/events/order/high-priority` | Create high-priority order |
| GET | `/api/events/order/{orderId}` | Get order events |
| GET | `/api/events/order/user/{userId}` | Get orders by user |
| GET | `/api/events/order/high-value` | Get high-value orders |
| GET | `/api/events/processed/type/{eventType}` | Get processed events |
| GET | `/api/events/errors/unresolved` | Get unresolved errors |
| POST | `/api/events/errors/{errorId}/resolve` | Resolve error |
| POST | `/api/events/test/user-events/{count}` | Generate test user events |
| POST | `/api/events/test/order-events/{count}` | Generate test order events |

---

## 📞 Support

For questions or issues:
1. Check the troubleshooting section above
2. Review application logs
3. Verify Kafka and MongoDB connections
4. Test with provided examples

This documentation provides a complete understanding of the Kafka Streams MongoDB application architecture and usage. Use it as a reference for development, testing, and production deployment.