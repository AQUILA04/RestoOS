package com.resto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestoOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestoOsApplication.class, args);
    }
}
