package com.ureka.team3.utong_scheduler.subscriber;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.config.RabbitMQConfig;
import com.ureka.team3.utong_scheduler.publisher.AlertPublisher;
import com.ureka.team3.utong_scheduler.publisher.TradeQueuePublisher;
import com.ureka.team3.utong_scheduler.trade.RequestType;
import com.ureka.team3.utong_scheduler.trade.alert.AlertService;
import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeExecutedMessage;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeMatch;
import com.ureka.team3.utong_scheduler.trade.queue.service.ContractQueueService;
import com.ureka.team3.utong_scheduler.trade.queue.service.TradeQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExecutedSubscriber {

    private final TradeQueueService tradeQueueService;
    private final TradeQueuePublisher tradeQueuePublisher;
    private final ContractQueueService contractQueueService;
    private final DataTradePolicy dataTradePolicy;
    private final AlertService alertService;
    private final AlertPublisher alertPublisher;

    @RabbitListener(queues = RabbitMQConfig.TRADE_EXECUTED_QUEUE)
    public void handleTradeExecutedMessage(TradeExecutedMessage message) {
        try {
            processTradeExecutedMessage(message);
            log.info("거래 체결 메시지 처리 완료");
        } catch (Exception e) {
            log.error("거래 체결 메시지 처리 실패", e);
            throw e;
        }
    }

    public void processTradeExecutedMessage(TradeExecutedMessage message) {
        try {
            Map<Long, Long> saleDataChanges = new HashMap<>();
            Map<Long, Long> purchaseDataChanges = new HashMap<>();
            List<TradeMatch> matchedList = message.getMatchedList();

            if (message.getRequestType().equals(RequestType.PURCHASE)) {
                purchaseDataChanges.put(message.getRequestPrice(), message.getRemain());

                if(matchedList!=null&&!matchedList.isEmpty()){
                    for (TradeMatch tradeMatch : matchedList) {
                        Long pricePerUnit = tradeMatch.getPricePerUnit();
                        Long amount = tradeMatch.getAmount();

                        saleDataChanges.put(pricePerUnit,
                                saleDataChanges.getOrDefault(pricePerUnit, 0L) - amount);
                    }
                }

            } else {
                saleDataChanges.put(message.getRequestPrice(), message.getRemain());
                if(matchedList!=null&&!matchedList.isEmpty()){
                    for (TradeMatch tradeMatch : matchedList) {
                        Long pricePerUnit = tradeMatch.getPricePerUnit();
                        Long amount = tradeMatch.getAmount();

                        purchaseDataChanges.put(pricePerUnit,
                                purchaseDataChanges.getOrDefault(pricePerUnit, 0L) - amount);
                    }
                }

            }

            tradeQueueService.changeCurrentDataAmount(message.getDataCode(), saleDataChanges, purchaseDataChanges);

            if (message.getNewContracts() != null && !message.getNewContracts().isEmpty()) {
                contractQueueService.addNewContracts(message.getDataCode(), message.getNewContracts());
                alertPublisher.publish(LocalDateTime.now(), alertService.buildAlertMessage(message));
//               emailNotificationSubscriber.sendContractNotificationEmails(tradeExecutedMessage); // 비동기 -> rabbitMQ 처리
            }

            Map<String, OrdersQueueDto> dataMap = new HashMap<>();
            for (Code code : dataTradePolicy.getDataTypeCodeList()) {
                Map<Long, Long> allBuyOrderNumbers = tradeQueueService.getAllBuyOrderCachedNumbers(code.getCode());
                Map<Long, Long> allSellOrderNumbers = tradeQueueService.getAllSellOrderCachedNumbers(code.getCode());
                List<ContractDto> recentContracts = contractQueueService.getRecentContracts(code.getCode());

                dataMap.put(code.getCode(), OrdersQueueDto.builder()
                        .buyOrderQuantity(allBuyOrderNumbers)
                        .sellOrderQuantity(allSellOrderNumbers)
                        .recentContracts(recentContracts)
                        .build());
            }

            tradeQueuePublisher.publish(LocalDateTime.now(), dataMap);
	    } catch(Exception e) {
	        tradeQueueService.init();
	        log.error("집계 완료 메시지 처리 중 오류: {}", e.getMessage(), e);
	    }
	}

//    private TradeExecutedMessage getTradeExecutedMessage(Message message) throws JsonProcessingException {
//        String payload = new String(message.getBody());
//        return getTradeExecutedMessage(payload);
//    }
//
//    private TradeExecutedMessage getTradeExecutedMessage(String payload) throws JsonProcessingException {
//        return objectMapper.readValue(payload, TradeExecutedMessage.class);
//    }
    
}
