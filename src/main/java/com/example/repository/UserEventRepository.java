package com.example.repository;

import com.example.model.UserEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserEventRepository extends MongoRepository<UserEvent, String> {
    
    List<UserEvent> findByUserId(String userId);
    
    List<UserEvent> findByEventType(String eventType);
    
    List<UserEvent> findByProcessed(boolean processed);
    
    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    List<UserEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{'userId': ?0, 'eventType': ?1}")
    List<UserEvent> findByUserIdAndEventType(String userId, String eventType);
    
    List<UserEvent> findByUserIdAndProcessed(String userId, boolean processed);
    
    long countByEventType(String eventType);
    
    long countByProcessed(boolean processed);
}