package com.ureka.team3.utong_scheduler.config;

import com.ureka.team3.utong_scheduler.contract.scheduler.ContractAggregationScheduler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final ContractAggregationScheduler contractAggregationScheduler;

    @PostConstruct
    void init() {
        contractAggregationScheduler.init();
    }
}