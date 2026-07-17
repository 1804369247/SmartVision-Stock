package com.example.smartvisionstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartVisionStockApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartVisionStockApplication.class, args);
    }
}
