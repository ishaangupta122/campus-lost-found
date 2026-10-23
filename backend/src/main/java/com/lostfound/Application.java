package com.lostfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point. @SpringBootApplication scans this package and its sub-packages. */
@SpringBootApplication
@EnableScheduling // enables @Scheduled (UploadCleanup)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
