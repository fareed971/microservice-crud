package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableKafka
@EnableKafkaStreams
@EnableAsync
public class KafkaStreamsMongoApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaStreamsMongoApplication.class, args);
    }
}