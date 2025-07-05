package com.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "error_events")
public class ErrorEvent {
    
    @Id
    private String id;
    
    @JsonProperty("original_event")
    private String originalEvent;
    
    @JsonProperty("original_topic")
    private String originalTopic;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("error_type")
    private String errorType;
    
    @JsonProperty("stack_trace")
    private String stackTrace;
    
    @JsonProperty("retry_count")
    private Integer retryCount = 0;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("processing_stage")
    private String processingStage;
    
    @JsonProperty("resolved")
    private boolean resolved = false;

    // Default constructor
    public ErrorEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with parameters
    public ErrorEvent(String originalEvent, String originalTopic, String errorMessage, 
                     String errorType, String processingStage) {
        this();
        this.originalEvent = originalEvent;
        this.originalTopic = originalTopic;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.processingStage = processingStage;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalEvent() {
        return originalEvent;
    }

    public void setOriginalEvent(String originalEvent) {
        this.originalEvent = originalEvent;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public void setOriginalTopic(String originalTopic) {
        this.originalTopic = originalTopic;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getProcessingStage() {
        return processingStage;
    }

    public void setProcessingStage(String processingStage) {
        this.processingStage = processingStage;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    @Override
    public String toString() {
        return "ErrorEvent{" +
                "id='" + id + '\'' +
                ", originalEvent='" + originalEvent + '\'' +
                ", originalTopic='" + originalTopic + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorType='" + errorType + '\'' +
                ", retryCount=" + retryCount +
                ", timestamp=" + timestamp +
                ", processingStage='" + processingStage + '\'' +
                ", resolved=" + resolved +
                '}';
    }
}