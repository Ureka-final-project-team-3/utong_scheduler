package com.ureka.team3.utong_scheduler.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.contract.dto.AggregationStatusMessage;
import com.ureka.team3.utong_scheduler.contract.dto.AvgPerHour;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private static final String CONTRACT_AGGREGATION_STATUS_CHANNEL = "contract:aggregation:status";
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publishAggregationComplete(LocalDateTime aggregatedAt, Map<String, List<AvgPerHour>> dataMap) {
        try {
            AggregationStatusMessage message = AggregationStatusMessage.builder()
                    .status("SUCCESS")
                    .aggregatedAt(aggregatedAt)
                    .publishedAt(LocalDateTime.now())
                    .message("계약 평균가 집계 완료")
                    .dataMap(dataMap)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CONTRACT_AGGREGATION_STATUS_CHANNEL, jsonMessage);

            log.info("집계 완료 메시지 발행: {}", aggregatedAt);
        } catch (Exception e) {
            log.error("집계 완료 메시지 발행 중 오류 발생: {}", e.getMessage());
        }
    }

    public void publishAggregationFailed(String errorMessage) {
        try {
            AggregationStatusMessage message = AggregationStatusMessage.builder()
                    .status("FAILED")
                    .aggregatedAt(null)
                    .publishedAt(LocalDateTime.now())
                    .message("계약 평균가 집계 실패" + errorMessage)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CONTRACT_AGGREGATION_STATUS_CHANNEL, jsonMessage);

            log.error("집계 실패 메시지 발행: {}", errorMessage);
        } catch (Exception e) {
            log.error("집계 실패 메시지 발행 중 오류 발생: {}", e.getMessage());
        }
    }
}
