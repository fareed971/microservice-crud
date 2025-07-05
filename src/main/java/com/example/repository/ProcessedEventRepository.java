package com.example.repository;

import com.example.model.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
    
    Optional<ProcessedEvent> findByOriginalEventId(String originalEventId);
    
    List<ProcessedEvent> findByEventType(String eventType);
    
    List<ProcessedEvent> findByStatus(String status);
    
    @Query("{'processingTimestamp': {$gte: ?0, $lte: ?1}}")
    List<ProcessedEvent> findByProcessingTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{'originalTimestamp': {$gte: ?0, $lte: ?1}}")
    List<ProcessedEvent> findByOriginalTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    List<ProcessedEvent> findByEventTypeAndStatus(String eventType, String status);
    
    long countByEventType(String eventType);
    
    long countByStatus(String status);
    
    @Query("{'processingTimestamp': {$gte: ?0}}")
    List<ProcessedEvent> findProcessedAfter(LocalDateTime timestamp);
}