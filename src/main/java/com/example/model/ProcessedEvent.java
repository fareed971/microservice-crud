package com.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "processed_events")
public class ProcessedEvent {
    
    @Id
    private String id;
    
    @JsonProperty("original_event_id")
    private String originalEventId;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("processing_result")
    private String processingResult;
    
    @JsonProperty("enriched_data")
    private String enrichedData;
    
    @JsonProperty("processing_timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processingTimestamp;
    
    @JsonProperty("original_timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime originalTimestamp;
    
    @JsonProperty("status")
    private String status;

    // Default constructor
    public ProcessedEvent() {
        this.processingTimestamp = LocalDateTime.now();
        this.status = "PROCESSED";
    }

    // Constructor with parameters
    public ProcessedEvent(String originalEventId, String eventType, String processingResult, 
                         String enrichedData, LocalDateTime originalTimestamp) {
        this();
        this.originalEventId = originalEventId;
        this.eventType = eventType;
        this.processingResult = processingResult;
        this.enrichedData = enrichedData;
        this.originalTimestamp = originalTimestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalEventId() {
        return originalEventId;
    }

    public void setOriginalEventId(String originalEventId) {
        this.originalEventId = originalEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getProcessingResult() {
        return processingResult;
    }

    public void setProcessingResult(String processingResult) {
        this.processingResult = processingResult;
    }

    public String getEnrichedData() {
        return enrichedData;
    }

    public void setEnrichedData(String enrichedData) {
        this.enrichedData = enrichedData;
    }

    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }

    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }

    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }

    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ProcessedEvent{" +
                "id='" + id + '\'' +
                ", originalEventId='" + originalEventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", processingResult='" + processingResult + '\'' +
                ", enrichedData='" + enrichedData + '\'' +
                ", processingTimestamp=" + processingTimestamp +
                ", originalTimestamp=" + originalTimestamp +
                ", status='" + status + '\'' +
                '}';
    }
}