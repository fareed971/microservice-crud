// MongoDB Shell Script - Run this in MongoDB shell (mongo or mongosh)
// First connect to database with: use kafka_streams_db;

// Create collections with sample documents
print("Creating userEvents collection...");
db.userEvents.insertOne({
    "userId": "sample_user",
    "eventType": "USER_LOGIN",
    "userName": "Sample User",
    "email": "sample@example.com",
    "timestamp": new Date()
});

print("Creating orderEvents collection...");
db.orderEvents.insertOne({
    "orderId": "sample_order",
    "userId": "sample_user",
    "productName": "Sample Product",
    "quantity": 1,
    "price": 99.99,
    "totalAmount": 99.99,
    "timestamp": new Date()
});

print("Creating processedEvents collection...");
db.processedEvents.insertOne({
    "eventId": "sample_processed",
    "eventType": "PROCESSED",
    "description": "Sample processed event",
    "details": "This is a sample processed event",
    "timestamp": new Date()
});

print("Creating errorEvents collection...");
db.errorEvents.insertOne({
    "eventId": "sample_error",
    "eventType": "ERROR",
    "errorMessage": "Sample error message",
    "retryCount": 0,
    "resolved": false,
    "timestamp": new Date()
});

// Create indexes for better performance
print("Creating indexes...");
db.userEvents.createIndex({ "userId": 1 });
db.userEvents.createIndex({ "timestamp": 1 });
db.orderEvents.createIndex({ "orderId": 1 });
db.orderEvents.createIndex({ "userId": 1 });
db.orderEvents.createIndex({ "timestamp": 1 });
db.processedEvents.createIndex({ "timestamp": 1 });
db.errorEvents.createIndex({ "resolved": 1 });

// Display all collections
print("\nCollections created:");
db.getCollectionNames().forEach(collection => {
    print("- " + collection + " (documents: " + db.getCollection(collection).countDocuments() + ")");
});

print("\nMongoDB collections setup complete!");