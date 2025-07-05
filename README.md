# Spring Boot Kafka Streams with MongoDB Integration

A comprehensive Spring Boot application that demonstrates Kafka Streams processing with multiple producers, consumers, error handling, and MongoDB integration.

## Features

- **Multiple Kafka Producers**: UserEventProducer and OrderEventProducer with async/sync capabilities
- **Multiple Kafka Consumers**: Robust consumers with retry mechanisms and dead letter topics
- **Kafka Streams Processing**: Advanced stream processing with joins, aggregations, and windowing
- **MongoDB Integration**: Complete CRUD operations with Spring Data MongoDB
- **Error Handling**: Comprehensive error handling with retry logic and error topics
- **REST API**: Full REST API for testing and interaction
- **Health Monitoring**: Health checks and statistics endpoints

## Architecture

```
┌─────────────┐    ┌──────────────┐    ┌─────────────────┐    ┌─────────────┐
│   REST API  │───▶│   Producers  │───▶│  Kafka Topics   │───▶│  Consumers  │
└─────────────┘    └──────────────┘    └─────────────────┘    └─────────────┘
                                                │                      │
                                                ▼                      ▼
                                       ┌─────────────────┐    ┌─────────────┐
                                       │ Kafka Streams   │    │   MongoDB   │
                                       │   Processing    │    │  Database   │
                                       └─────────────────┘    └─────────────┘
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Apache Kafka (or use Docker)
- MongoDB (or use Docker)

## Quick Start with Docker

1. **Start Kafka and MongoDB using Docker Compose**:

```yaml
# docker-compose.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  mongodb:
    image: mongo:latest
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
```

2. **Start the services**:
```bash
docker-compose up -d
```

3. **Build and run the application**:
```bash
mvn clean package
java -jar target/kafka-streams-mongodb-1.0.0.jar
```

## Configuration

The application can be configured via `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
  data:
    mongodb:
      host: localhost
      port: 27017
      database: kafka_streams_db

app:
  kafka:
    topics:
      user-events: user-events
      order-events: order-events
      processed-events: processed-events
      error-events: error-events
      dead-letter: dead-letter-topic
```

## API Endpoints

### User Events

- **POST** `/api/events/user` - Create user event (async)
- **POST** `/api/events/user/sync` - Create user event (sync)
- **GET** `/api/events/user/{userId}` - Get user events by user ID
- **GET** `/api/events/user/type/{eventType}` - Get user events by type
- **GET** `/api/events/user/unprocessed` - Get unprocessed user events

### Order Events

- **POST** `/api/events/order` - Create order event
- **POST** `/api/events/order/high-priority` - Create high-priority order event
- **GET** `/api/events/order/{orderId}` - Get order events by order ID
- **GET** `/api/events/order/user/{userId}` - Get order events by user ID
- **GET** `/api/events/order/type/{eventType}` - Get order events by type
- **GET** `/api/events/order/high-value` - Get high-value orders

### Processed Events

- **GET** `/api/events/processed/type/{eventType}` - Get processed events by type
- **GET** `/api/events/processed/{originalEventId}` - Get processed event by original ID

### Error Events

- **GET** `/api/events/errors/unresolved` - Get unresolved errors
- **GET** `/api/events/errors/type/{errorType}` - Get errors by type
- **POST** `/api/events/errors/{errorId}/resolve` - Mark error as resolved

### Test Data Generation

- **POST** `/api/events/test/user-events/{count}` - Generate test user events
- **POST** `/api/events/test/order-events/{count}` - Generate test order events
- **POST** `/api/events/test/high-value-orders/{count}` - Generate test high-value orders

### Health and Statistics

- **GET** `/api/events/health` - Health check
- **GET** `/api/events/stats` - Event statistics

## Usage Examples

### 1. Create a User Event

```bash
curl -X POST "http://localhost:8080/api/events/user" \
  -d "userId=user123" \
  -d "eventType=USER_CREATED" \
  -d "userName=John Doe" \
  -d "email=john@example.com"
```

### 2. Create an Order Event

```bash
curl -X POST "http://localhost:8080/api/events/order" \
  -d "orderId=order456" \
  -d "userId=user123" \
  -d "eventType=ORDER_CREATED" \
  -d "productName=Laptop" \
  -d "quantity=1" \
  -d "price=1200.00"
```

### 3. Generate Test Data

```bash
# Generate 10 user events
curl -X POST "http://localhost:8080/api/events/test/user-events/10"

# Generate 10 order events
curl -X POST "http://localhost:8080/api/events/test/order-events/10"

# Generate 5 high-value order events
curl -X POST "http://localhost:8080/api/events/test/high-value-orders/5"
```

### 4. Check Statistics

```bash
curl -X GET "http://localhost:8080/api/events/stats"
```

### 5. Check Application Health

```bash
curl -X GET "http://localhost:8080/api/events/health"
```

## Kafka Streams Features

The application includes several advanced Kafka Streams features:

### 1. Stream Processing
- **Filtering**: High-value orders are automatically flagged
- **Mapping**: Events are transformed and enriched
- **Aggregation**: Event counts are calculated in time windows

### 2. Joins
- **Stream-Stream Join**: User events are joined with order events
- **Global Table Join**: Orders are enriched with user information

### 3. Windowing
- **Time Windows**: Event counts are aggregated in 5-minute windows
- **Session Windows**: User session tracking for login/logout events

### 4. State Stores
- **Key-Value Store**: User session information is maintained
- **Global Tables**: User lookup for order enrichment

## Error Handling

The application includes comprehensive error handling:

### 1. Retry Mechanism
- Automatic retries with exponential backoff
- Configurable retry attempts and delays

### 2. Dead Letter Topics
- Failed messages are sent to dead letter topics
- Manual investigation and reprocessing capabilities

### 3. Error Events
- All errors are captured and stored in MongoDB
- Error resolution tracking and management

## MongoDB Collections

The application creates the following MongoDB collections:

- `user_events` - User event data
- `order_events` - Order event data  
- `processed_events` - Successfully processed events
- `error_events` - Error information and failed events

## Monitoring and Observability

### Logging
- Structured logging with correlation IDs
- Different log levels for different components
- Error tracking and debugging information

### Metrics
- Event processing statistics
- Error rates and counts
- Performance metrics

### Health Checks
- Kafka connectivity
- MongoDB connectivity
- Overall application health

## Development

### Running Tests
```bash
mvn test
```

### Building the Application
```bash
mvn clean package
```

### Running in Development Mode
```bash
mvn spring-boot:run
```

## Production Considerations

### 1. Configuration
- Adjust Kafka partition counts based on throughput requirements
- Configure appropriate replication factors for topics
- Tune consumer and producer settings

### 2. Scaling
- Increase consumer concurrency for higher throughput
- Scale horizontally by running multiple application instances
- Use Kafka consumer groups for load distribution

### 3. Monitoring
- Implement proper monitoring and alerting
- Track key metrics like processing latency and error rates
- Use tools like Prometheus and Grafana for visualization

### 4. Security
- Enable Kafka security features (SSL, SASL)
- Secure MongoDB with authentication
- Implement API authentication and authorization

## Troubleshooting

### Common Issues

1. **Kafka Connection Issues**
   - Check if Kafka is running on localhost:9092
   - Verify network connectivity
   - Check firewall settings

2. **MongoDB Connection Issues**
   - Ensure MongoDB is running on localhost:27017
   - Check database permissions
   - Verify connection string

3. **Serialization Issues**
   - Check Jackson configuration
   - Verify model class annotations
   - Review trusted packages configuration

### Debugging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.example: DEBUG
    org.apache.kafka: DEBUG
    org.springframework.kafka: DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.