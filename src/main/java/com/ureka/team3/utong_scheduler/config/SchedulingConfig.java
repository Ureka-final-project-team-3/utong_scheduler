package com.ureka.team3.utong_scheduler.config;

import com.ureka.team3.utong_scheduler.trade.chart.scheduler.ContractAggregationScheduler;
import com.ureka.team3.utong_scheduler.trade.chart.service.CurrentPriceService;
import com.ureka.team3.utong_scheduler.trade.queue.service.ContractQueueService;
import com.ureka.team3.utong_scheduler.trade.queue.service.TradeQueueService;
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
    private final TradeQueueService tradeQueueService;
    private final ContractQueueService contractQueueService;

    @PostConstruct
    void init() {
        contractAggregationScheduler.insertInitialData();
        currentPriceService.init();
        tradeQueueService.init();
        contractQueueService.initAllRecentContracts();
    }
}