package com.ureka.team3.utong_scheduler.trade.chart.scheduler;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.chart.dto.AvgPerHour;
import com.ureka.team3.utong_scheduler.trade.chart.service.AggregationService;
import com.ureka.team3.utong_scheduler.trade.chart.service.CurrentPriceService;
import com.ureka.team3.utong_scheduler.publisher.AggregationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractAggregationScheduler {

    private final AggregationPublisher aggregationPublisher;
    private final DataTradePolicy dataTradePolicy;
    private final AggregationService aggregationService;
    private final CurrentPriceService currentPriceService;

    // 매시간 계약 평균가를 계산하여 저장하는 스케줄러
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void handleAggregation() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime previousHour = currentHour.minusHours(1);


        try {
            Map<String, List<AvgPerHour>> dataMap = new HashMap<>();

            for (Code code : dataTradePolicy.getDataTypeCodeList()) {
                String dataCode = code.getCode();

                aggregationService.aggregateHourly(currentHour, previousHour, dataCode);
                currentPriceService.updateRedisCache(dataCode,currentHour);

                List<AvgPerHour> updatedData = currentPriceService.getUpdatedData(dataCode);
                dataMap.put(dataCode, updatedData);
            }

            // ✅ 하나의 메시지로 여러 코드 데이터 전송
            aggregationPublisher.publish(currentHour, dataMap);

        } catch (Exception e) {
            log.error("계약 평균가 집계 중 오류 발생: {}", e.getMessage(), e);
            aggregationPublisher.noticeFailed(e.getMessage());
        }
    }

    @Transactional
    public void insertInitialData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime previousHour = currentHour.minusHours(1);
        LocalDateTime initialHour = currentHour.minusHours(DataTradePolicy.CHART_LIST_SIZE);

        for (Code code : dataTradePolicy.getDataTypeCodeList()) {
            int count = aggregationService.getDataCountInRange(initialHour, currentHour, code.getCode());
            int insertCount = DataTradePolicy.CHART_LIST_SIZE - count - 1;

            for (int i = insertCount; i >= 0; i--) {
                aggregationService.aggregateHourly(
                        currentHour.minusHours(i),
                        previousHour.minusHours(i),
                        code.getCode()
                );
            }
        }
    }
}
