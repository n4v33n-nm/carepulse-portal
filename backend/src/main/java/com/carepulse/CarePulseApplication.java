package com.carepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CarePulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarePulseApplication.class, args);
    }
}
