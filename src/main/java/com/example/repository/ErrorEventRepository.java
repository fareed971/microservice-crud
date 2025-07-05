package com.example.repository;

import com.example.model.ErrorEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ErrorEventRepository extends MongoRepository<ErrorEvent, String> {
    
    List<ErrorEvent> findByOriginalTopic(String originalTopic);
    
    List<ErrorEvent> findByErrorType(String errorType);
    
    List<ErrorEvent> findByProcessingStage(String processingStage);
    
    List<ErrorEvent> findByResolved(boolean resolved);
    
    @Query("{'retryCount': {$gte: ?0}}")
    List<ErrorEvent> findByRetryCountGreaterThanEqual(Integer retryCount);
    
    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    List<ErrorEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    List<ErrorEvent> findByErrorTypeAndResolved(String errorType, boolean resolved);
    
    List<ErrorEvent> findByOriginalTopicAndResolved(String originalTopic, boolean resolved);
    
    long countByErrorType(String errorType);
    
    long countByResolved(boolean resolved);
    
    @Query("{'retryCount': {$lt: ?0}, 'resolved': false}")
    List<ErrorEvent> findRetryableErrors(Integer maxRetries);
}