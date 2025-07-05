# 🚀 Quick Reference Guide

## 📋 Essential Commands

### **Start Application**
```bash
# 1. Start infrastructure
docker-compose up -d zookeeper kafka kafka-ui

# 2. Start application
mvn spring-boot:run

# 3. Verify health
curl http://localhost:8080/api/events/health
```

### **Generate Test Data**
```bash
# Generate user events
curl -X POST http://localhost:8080/api/events/test/user-events/5

# Generate order events
curl -X POST http://localhost:8080/api/events/test/order-events/5

# Generate high-value orders
curl -X POST http://localhost:8080/api/events/test/high-value-orders/3
```

### **View Data**
```bash
# User events
curl http://localhost:8080/api/events/user/unprocessed

# Order events
curl http://localhost:8080/api/events/order/high-value

# Application stats
curl http://localhost:8080/api/events/stats
```

## 🔗 Access URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Application API | http://localhost:8080 | REST endpoints |
| Health Check | http://localhost:8080/api/events/health | Application health |
| Statistics | http://localhost:8080/api/events/stats | Real-time metrics |
| Kafka UI | http://localhost:8081 | Topic management |
| MongoDB Express | http://localhost:8082 | Database browser |

## 📊 Kafka Topics

| Topic | Purpose | Auto-Created |
|-------|---------|--------------|
| user-events | User activities | ✅ |
| order-events | Order lifecycle | ✅ |
| processed-events | Stream processing results | ✅ |
| error-events | Error tracking | ✅ |
| dead-letter-topic | Failed messages | ✅ |

### **Access Topics via Kafka UI**
1. Open http://localhost:8081
2. Navigate to "Topics" section
3. Click on any topic to view messages
4. Use "Producers" tab to send test messages

### **Access Topics via Command Line**
```bash
# List all topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# View messages in user-events topic
docker exec kafka kafka-console-consumer --topic user-events --bootstrap-server localhost:9092 --from-beginning

# Send test message
echo '{"userId":"test","eventType":"USER_LOGIN","userName":"Test","email":"test@example.com"}' | docker exec -i kafka kafka-console-producer --topic user-events --bootstrap-server localhost:9092
```

## 🗄️ MongoDB Collections

| Collection | Purpose | Auto-Created |
|------------|---------|--------------|
| userEvents | User event storage | ✅ |
| orderEvents | Order event storage | ✅ |
| processedEvents | Processed event tracking | ✅ |
| errorEvents | Error logging | ✅ |

### **Access MongoDB**
```bash
# Via MongoDB shell
mongo
use kafka_streams_db
show collections
db.userEvents.find().pretty()

# Via MongoDB Express (browser)
# http://localhost:8082
# Username: admin, Password: admin123
```

## 🔄 Data Flow Examples

### **1. Complete User Event Flow**
```bash
# 1. Create user event via API
curl -X POST "http://localhost:8080/api/events/user" \
  -d "userId=user1&eventType=USER_LOGIN&userName=John Doe&email=john@example.com"

# 2. Check in Kafka (Kafka UI: http://localhost:8081)
# 3. Check in MongoDB
mongo
use kafka_streams_db
db.userEvents.find({"userId": "user1"})

# 4. Check processed events
curl http://localhost:8080/api/events/user/user1
```

### **2. Order Processing Flow**
```bash
# 1. Create high-value order
curl -X POST "http://localhost:8080/api/events/order" \
  -d "orderId=order1&userId=user1&eventType=ORDER_CREATED&productName=Server&quantity=1&price=5000"

# 2. Check stream processing (high-value detection)
curl http://localhost:8080/api/events/order/high-value

# 3. Check processed events
curl http://localhost:8080/api/events/processed/type/HIGH_VALUE_ORDER
```

## 🔧 Troubleshooting Quick Fixes

### **Application Won't Start**
```bash
# Check if ports are free
netstat -an | findstr "8080\|9092\|27017"

# Restart infrastructure
docker-compose down
docker-compose up -d
```

### **No Data in MongoDB**
```bash
# Check if application is connected
curl http://localhost:8080/api/events/health

# Generate test data
curl -X POST http://localhost:8080/api/events/test/user-events/3

# Verify in MongoDB
mongo
use kafka_streams_db
db.userEvents.count()
```

### **Kafka Not Working**
```bash
# Check Kafka status
docker logs kafka

# Test Kafka directly
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

## 📈 Monitoring Commands

### **Application Health**
```bash
curl http://localhost:8080/api/events/health
# Expected: {"status":"UP","kafka":"UP","timestamp":...}
```

### **Real-time Statistics**
```bash
curl http://localhost:8080/api/events/stats
# Shows: total events, errors, event type counts
```

### **Error Monitoring**
```bash
curl http://localhost:8080/api/events/errors/unresolved
# Shows: unresolved error events
```

## 🎯 Testing Scenarios

### **Scenario 1: Basic Event Processing**
```bash
curl -X POST http://localhost:8080/api/events/test/user-events/3
curl -X POST http://localhost:8080/api/events/test/order-events/3
curl http://localhost:8080/api/events/stats
```

### **Scenario 2: High-Value Order Detection**
```bash
curl -X POST "http://localhost:8080/api/events/order" \
  -d "orderId=test&userId=test&eventType=ORDER_CREATED&productName=Laptop&quantity=1&price=2000"
curl http://localhost:8080/api/events/order/high-value
```

### **Scenario 3: Error Handling**
```bash
# This will trigger error handling
curl -X POST "http://localhost:8080/api/events/user" \
  -d "userId=&eventType=INVALID&userName=&email="
curl http://localhost:8080/api/events/errors/unresolved
```

---

## 📞 Need Help?

1. **Check Application Logs**: Look at console output where `mvn spring-boot:run` is running
2. **Check Infrastructure**: `docker ps` and `docker logs kafka`
3. **Verify Connections**: Use health endpoints and Kafka/MongoDB UIs
4. **Review Full Documentation**: See `PROJECT_DOCUMENTATION.md` for detailed explanations