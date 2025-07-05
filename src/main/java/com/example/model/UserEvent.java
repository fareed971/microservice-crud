package com.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_events")
public class UserEvent {
    
    @Id
    private String id;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("user_name")
    private String userName;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("metadata")
    private String metadata;
    
    @JsonProperty("processed")
    private boolean processed = false;

    // Default constructor
    public UserEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with parameters
    public UserEvent(String userId, String eventType, String userName, String email) {
        this();
        this.userId = userId;
        this.eventType = eventType;
        this.userName = userName;
        this.email = email;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    @Override
    public String toString() {
        return "UserEvent{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", timestamp=" + timestamp +
                ", metadata='" + metadata + '\'' +
                ", processed=" + processed +
                '}';
    }
}