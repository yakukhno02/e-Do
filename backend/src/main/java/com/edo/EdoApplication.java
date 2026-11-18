package com.edo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EdoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdoApplication.class, args);
    }

}
