package com.example.repository;

import com.example.model.OrderEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderEventRepository extends MongoRepository<OrderEvent, String> {
    
    List<OrderEvent> findByOrderId(String orderId);
    
    List<OrderEvent> findByUserId(String userId);
    
    List<OrderEvent> findByEventType(String eventType);
    
    List<OrderEvent> findByStatus(String status);
    
    List<OrderEvent> findByProcessed(boolean processed);
    
    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    List<OrderEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{'totalAmount': {$gte: ?0}}")
    List<OrderEvent> findByTotalAmountGreaterThanEqual(BigDecimal amount);
    
    List<OrderEvent> findByUserIdAndEventType(String userId, String eventType);
    
    List<OrderEvent> findByUserIdAndProcessed(String userId, boolean processed);
    
    long countByEventType(String eventType);
    
    long countByStatus(String status);
    
    @Query("{'totalAmount': {$gte: ?0, $lte: ?1}}")
    List<OrderEvent> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
}