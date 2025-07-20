package com.ureka.team3.utong_scheduler.contract.service;


import com.ureka.team3.utong_scheduler.contract.dto.AvgPerHour;

import java.time.LocalDateTime;
import java.util.List;

public interface CurrentPriceService {

    void updateRedisCache(LocalDateTime aggregatedAt);

    List<AvgPerHour> getUpdatedData(String dataCode);
}
