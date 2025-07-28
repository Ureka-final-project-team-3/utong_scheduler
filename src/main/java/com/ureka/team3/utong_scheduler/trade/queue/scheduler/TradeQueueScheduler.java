//package com.ureka.team3.utong_scheduler.trade.queue.scheduler;
//
//import com.ureka.team3.utong_scheduler.common.entity.Code;
//import com.ureka.team3.utong_scheduler.publisher.TradeQueuePublisher;
//import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
//import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
//import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
//import com.ureka.team3.utong_scheduler.trade.queue.service.ContractQueueService;
//import com.ureka.team3.utong_scheduler.trade.queue.service.TradeQueueService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TradeQueueScheduler {
//
//    private final TradeQueuePublisher tradeQueuePublisher;
//    private final ContractQueueService contractQueueService;
//    private final DataTradePolicy dataTradePolicy;
//    private final TradeQueueService tradeQueueService;
//
//    // 매시간 계약 평균가를 계산하여 저장하는 스케줄러
//    @Scheduled(cron = "0/5 * * * * *")
//    @Transactional
//    public void handleAggregation() {
//        try {
//            // 2. 금액별로 price 다 더해서 데이터 코드 : 판매 or 구매 : 금액 키로 저장
//            // 3. redis pubsub으로 다 보내주기
//            Map<String, OrdersQueueDto> dataMap = new HashMap<>();
//
//            // # 데이터 코드 (001, 002) 마다 진행
//            for (Code code : dataTradePolicy.getDataTypeCodeList()) {
//                // 1. 레디스에서 구매 대기열, 판매 대기열 불러오기
//                String dataCode = code.getCode();
//                Map<Long, Long> allBuyOrderNumbers = tradeQueueService.getAllBuyOrderNumbers(dataCode);
//                Map<Long, Long> allSellOrderNumbers = tradeQueueService.getAllSellOrderNumbers(dataCode);
//
//                List<ContractDto> recentContracts = contractQueueService.getRecentContracts(dataCode);
//
//                dataMap.put(dataCode,OrdersQueueDto.builder()
//                        .buyOrderQuantity(allBuyOrderNumbers)
//                        .sellOrderQuantity(allSellOrderNumbers)
//                        .recentContracts(recentContracts)
//                        .build()
//                );
//            }
//
//            // 하나의 메시지로 여러 코드 데이터 전송
//            tradeQueueService.initAllOrdersNumber(dataMap);
//            contractQueueService.initAllRecentContracts();
//
//            tradeQueuePublisher.publish(LocalDateTime.now(),dataMap);
//
//        } catch (Exception e) {
//            log.error("계약 평균가 집계 중 오류 발생: {}", e.getMessage(), e);
//            tradeQueuePublisher.noticeFailed(e.getMessage());
//        }
//    }
//
////    @Transactional
////    public void insertInitialData() {
////        LocalDateTime now = LocalDateTime.now();
////        LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
////        LocalDateTime previousHour = currentHour.minusHours(1);
////        LocalDateTime initialHour = currentHour.minusHours(DataTradePolicy.CHART_LIST_SIZE);
////
////        for (Code code : dataTradePolicy.getDataTypeCodeList()) {
////            int count = aggregationService.getDataCountInRange(initialHour, currentHour, code.getCode());
////            int insertCount = DataTradePolicy.CHART_LIST_SIZE - count - 1;
////
////            for (int i = insertCount; i >= 0; i--) {
////                aggregationService.aggregateHourly(
////                        currentHour.minusHours(i),
////                        previousHour.minusHours(i),
////                        code.getCode()
////                );
////            }
////        }
////    }
//}
