package com.app.myblogpusher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MyblogpusherApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyblogpusherApplication.class, args);
    }
}