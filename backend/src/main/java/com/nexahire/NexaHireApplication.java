package com.nexahire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NexaHireApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexaHireApplication.class, args);
    }
}
