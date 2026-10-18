package com.library.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotations.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling

public class LibraryBackendApplication {

    public static void main(String args[]){
        SpringApplication.run(LibraryBackendApplication.class, args);
    }
}