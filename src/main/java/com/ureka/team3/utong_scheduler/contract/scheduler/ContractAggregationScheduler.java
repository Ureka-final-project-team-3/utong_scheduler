package com.ureka.team3.utong_scheduler.contract.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.contract.dto.AggregationStatusMessage;
import com.ureka.team3.utong_scheduler.contract.repository.ContractHourlyAvgPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractAggregationScheduler {

    private final ContractHourlyAvgPriceRepository contractHourlyAvgPriceRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CONTRACT_AGGREGATION_STATUS_CHANNEL = "contract:aggregation:status";

    // 매시간 계약 평균가를 계산하여 저장하는 스케줄러
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void aggregateHourlyAvgPrice() {
        try {
            log.info("계약 평균가 집계 시작");

            // 현재 시간과 1시간 전 시간 계산
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
            LocalDateTime previousHour = now.minusHours(1).withMinute(0).withSecond(0).withNano(0);

            // 평균가 계산 및 저장
            contractHourlyAvgPriceRepository.insertHourlyAvgPrice(previousHour, currentHour);

            log.info("계약 평균가 집계 완료: {} ~ {}", previousHour, currentHour);

            publishAggregationComplete(previousHour);
        }
        catch (Exception e) {
            log.error("계약 평균가 집계 중 오류 발생: {}", e.getMessage());

            publishAggregationFailed(e.getMessage());
        }
    }

    private void publishAggregationComplete(LocalDateTime aggregatedAt) {
        try {
            AggregationStatusMessage message = AggregationStatusMessage.builder()
                    .status("SUCCESS")
                    .aggregatedAt(aggregatedAt)
                    .publishedAt(LocalDateTime.now())
                    .message("계약 평균가 집계 완료")
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CONTRACT_AGGREGATION_STATUS_CHANNEL, jsonMessage);

            log.info("집계 완료 메시지 발행: {}", aggregatedAt);
        } catch (Exception e) {
            log.error("집계 완료 메시지 발행 중 오류 발생: {}", e.getMessage());
        }
    }

    private void publishAggregationFailed(String errorMessage) {
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
