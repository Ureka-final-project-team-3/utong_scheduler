package com.ureka.team3.utong_scheduler.contract.scheduler;

import com.ureka.team3.utong_scheduler.contract.repository.ContractHourlyAvgPriceRepository;
import com.ureka.team3.utong_scheduler.price.entity.Price;
import com.ureka.team3.utong_scheduler.price.repository.PriceRepository;
import com.ureka.team3.utong_scheduler.publisher.RedisPublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RedisPublisher redisPublisher;

    private static final String PRICE_ID = "903ee67c-71b3-432e-bbd1-aaf5d5043376";
    private static final int MAX_REDIS_LIST_SIZE = 8;

    // 매시간 계약 평균가를 계산하여 저장하는 스케줄러
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void handleAggregation() {
        try {
            log.info("계약 평균가 집계 시작");

            // 현재 시간과 1시간 전 시간 계산
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
            LocalDateTime previousHour = now.minusHours(1).withMinute(0).withSecond(0).withNano(0);



            handleAggregation(currentHour, previousHour, "001"); // LTE
            handleAggregation(currentHour, previousHour, "002"); // 5G

            redisPublisher.publishAggregationComplete(currentHour);
        }
        catch (Exception e) {
            log.error("계약 평균가 집계 중 오류 발생: {}", e.getMessage());

            redisPublisher.publishAggregationFailed(e.getMessage());
        }
    }

    @Transactional
    public void handleAggregation(LocalDateTime currentHour, LocalDateTime previousHour, String dataCode) {
        // 해당 시간대 거래 건수 확인
        int contractCount = contractHourlyAvgPriceRepository.countContractsByTimeRange(previousHour, currentHour, dataCode);

        // 평균가 계산 및 저장
        if (contractCount > 0) { // 최근 1시간 거래가 있는 경우
            contractHourlyAvgPriceRepository.insertHourlyAvgPrice(previousHour, currentHour, dataCode);
            log.info("계약 평균가 집계 완료: {}건, {} ~ {}, 데이터 유형 : {}", contractCount, previousHour, currentHour, dataCode.equals("001") ? "LTE" : "5G");
        } else { // 최근 1시간 거래가 없는 경우 -> 이전 시간의 평균가 중 가장 가까운 시간의 평균가를 사용
            Long previousPrice = contractHourlyAvgPriceRepository.findLatestAvgPrice(previousHour, dataCode);

            if(previousPrice != null) {
                contractHourlyAvgPriceRepository.insertHourlyAvgPriceWithValue(currentHour, previousPrice, dataCode);
                log.info("이전 가격 기반 집계 완료: {} 원, 데이터 유형 : {}", previousPrice, dataCode.equals("001") ? "LTE" : "5G");
            }
            else { // 아무 거래도 없는 경우
                Price price = priceRepository.findById(PRICE_ID)
                        .orElseThrow(() -> new RuntimeException("기본 가격 정보가 없습니다."));

                contractHourlyAvgPriceRepository.insertHourlyAvgPriceWithValue(currentHour, price.getMinimumPrice(), dataCode);
                log.info("기본 가격 기반 집계 완료: {} 원, 데이터 유형 : {}", price.getMinimumPrice(), dataCode.equals("001") ? "LTE" : "5G");
            }
        }

    }

    @Transactional
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime previousHour = now.minusHours(1).withMinute(0).withSecond(0).withNano(0);

        for(int i = MAX_REDIS_LIST_SIZE - 1; i >= 0; i--) {
            handleAggregation(currentHour.minusHours(i), previousHour.minusHours(i), "001"); // LTE
            handleAggregation(currentHour.minusHours(i), previousHour.minusHours(i), "002"); // 5G
        }
    }
}
