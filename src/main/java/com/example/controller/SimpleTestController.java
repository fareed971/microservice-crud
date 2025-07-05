package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class SimpleTestController {
    
    @GetMapping("/health")
    public String health() {
        return "Application is running successfully!";
    }
    
    @GetMapping("/mongo")
    public String testMongo() {
        return "MongoDB connection test - implement if needed";
    }
    
    @GetMapping("/kafka")
    public String testKafka() {
        return "Kafka connection test - implement if needed";
    }
}