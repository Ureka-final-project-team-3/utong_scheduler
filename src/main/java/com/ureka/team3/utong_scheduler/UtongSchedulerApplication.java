package com.ureka.team3.utong_scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
public class UtongSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtongSchedulerApplication.class, args);
    }

}
