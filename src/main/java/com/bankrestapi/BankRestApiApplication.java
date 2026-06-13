package com.bankrestapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankRestApiApplication.class, args);
    }

}
