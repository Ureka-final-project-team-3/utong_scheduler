package com.ureka.team3.utong_scheduler.trade.chart.service;

import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

public interface AggregationService {
    @Transactional
    void aggregateHourly(LocalDateTime currentHour, LocalDateTime previousHour, String dataCode);

    int getDataCountInRange(LocalDateTime from, LocalDateTime to, String code);
}
