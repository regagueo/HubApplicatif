package com.hub.conges;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CongesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CongesServiceApplication.class, args);
    }
}
