package com.ureka.team3.utong_scheduler.trade.chart.service;


import com.ureka.team3.utong_scheduler.trade.chart.dto.AvgPerHour;

import java.time.LocalDateTime;
import java.util.List;

public interface CurrentPriceService {

    void updateRedisCache(String dataCode, LocalDateTime aggregatedAt);

    List<AvgPerHour> getUpdatedData(String dataCode);

    void init();
}
