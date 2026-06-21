package com.wade.wadeapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class WadeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WadeApiApplication.class, args);
    }

}
