package com.ureka.team3.utong_scheduler.config;

import com.ureka.team3.utong_scheduler.trade.chart.scheduler.ContractAggregationScheduler;
import com.ureka.team3.utong_scheduler.trade.chart.service.CurrentPriceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final ContractAggregationScheduler contractAggregationScheduler;
    private final CurrentPriceService currentPriceService;

    @PostConstruct
    void init() {
        contractAggregationScheduler.insertInitialData();
        currentPriceService.init();
    }
}