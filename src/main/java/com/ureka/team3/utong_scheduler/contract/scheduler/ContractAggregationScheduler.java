package com.ureka.team3.utong_scheduler.contract.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.contract.dto.AggregationStatusMessage;
import com.ureka.team3.utong_scheduler.contract.repository.ContractHourlyAvgPriceRepository;
import com.ureka.team3.utong_scheduler.price.entity.Price;
import com.ureka.team3.utong_scheduler.price.repository.PriceRepository;
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
    private final PriceRepository priceRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CONTRACT_AGGREGATION_STATUS_CHANNEL = "contract:aggregation:status";
    private static final String PRICE_ID = "903ee67c-71b3-432e-bbd1-aaf5d5043376";

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

            // 해당 시간대 거래 건수 확인
            int contractCount = contractHourlyAvgPriceRepository.countContractsByTimeRange(previousHour, currentHour);

            // 평균가 계산 및 저장
            if (contractCount > 0) { // 최근 1시간 거래가 있는 경우
                contractHourlyAvgPriceRepository.insertHourlyAvgPrice(previousHour, currentHour);
                log.info("계약 평균가 집계 완료: {}건, {} ~ {}", contractCount, previousHour, currentHour);
            } else { // 최근 1시간 거래가 없는 경우 -> 이전 시간의 평균가 중 가장 가까운 시간의 평균가를 사용
                Long previousPrice = contractHourlyAvgPriceRepository.findLatestAvgPrice(previousHour);

                if(previousPrice != null) {
                    contractHourlyAvgPriceRepository.insertHourlyAvgPriceWithValue(previousHour, previousPrice);
                    log.info("이전 가격 기반 집계 완료: {} 원", previousPrice);
                }
                else { // 아무 거래도 없는 경우
                    Price price = priceRepository.findById(PRICE_ID)
                            .orElseThrow(() -> new RuntimeException("기본 가격 정보가 없습니다."));

                    contractHourlyAvgPriceRepository.insertHourlyAvgPriceWithValue(previousHour, price.getMinimumPrice());
                    log.info("기본 가격 기반 집계 완료: {} 원", price.getMinimumPrice());
                }
            }

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
