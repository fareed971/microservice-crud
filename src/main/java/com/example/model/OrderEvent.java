package com.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "order_events")
public class OrderEvent {
    
    @Id
    private String id;
    
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("product_name")
    private String productName;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("processed")
    private boolean processed = false;

    // Default constructor
    public OrderEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with parameters
    public OrderEvent(String orderId, String userId, String eventType, String productName, 
                     Integer quantity, BigDecimal price) {
        this();
        this.orderId = orderId;
        this.userId = userId;
        this.eventType = eventType;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = price.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "id='" + id + '\'' +
                ", orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", totalAmount=" + totalAmount +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", processed=" + processed +
                '}';
    }
}