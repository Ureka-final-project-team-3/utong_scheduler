package com.ureka.team3.utong_scheduler.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.publisher.TradeQueuePublisher;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.queue.dto.*;
import com.ureka.team3.utong_scheduler.trade.queue.service.ContractQueueService;
import com.ureka.team3.utong_scheduler.trade.queue.service.TradeQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExecutedSubscriber implements MessageListener {   // 평균 시세 그래프 집계 완료 시 호출 (1시간 마다)
    private final ObjectMapper objectMapper;
    private final TradeQueueService tradeQueueService;
    private final TradeQueuePublisher tradeQueuePublisher;
    private final ContractQueueService contractQueueService;
    private final DataTradePolicy dataTradePolicy;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            log.info("집계 완료 메시지 수신: {}", message);
            TradeExecutedMessage tradeExecutedMessage = getTradeExecutedMessage(message);
            String dataCode = tradeExecutedMessage.getDataCode();
            Long purchaseDataChange = tradeExecutedMessage.getPurchaseDataChange();
            Long saleDataChange = tradeExecutedMessage.getSaleDataChange();
            Long price = tradeExecutedMessage.getPrice();
            tradeQueueService.changeCurrentDataAmount(dataCode, price, saleDataChange, purchaseDataChange);
            if(tradeExecutedMessage.getNewContracts()!=null && !tradeExecutedMessage.getNewContracts().isEmpty()){
                contractQueueService.addNewContracts(dataCode,tradeExecutedMessage.getNewContracts());
            }

            Map<String,OrdersQueueDto> dataMap = new HashMap<>();
            for (Code code : dataTradePolicy.getDataTypeCodeList()) {
                Map<Long, Long> allBuyOrderNumbers = tradeQueueService.getAllSellOrderCachedNumbers(code.getCode());
                Map<Long, Long> allSellOrderNumbers = tradeQueueService.getAllSellOrderCachedNumbers(code.getCode());
                List<ContractDto> recentContracts = contractQueueService.getRecentContracts(code.getCode());

                dataMap.put(dataCode,OrdersQueueDto.builder()
                        .buyOrderQuantity(allBuyOrderNumbers)
                        .sellOrderQuantity(allSellOrderNumbers)
                        .recentContracts(recentContracts)
                        .build());
            }
            tradeQueuePublisher.publish(LocalDateTime.now(),dataMap);
        } catch (Exception e) {
            tradeQueueService.init();
            log.error("집계 완료 메시지 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    private TradeExecutedMessage getTradeExecutedMessage(Message message) throws JsonProcessingException {
        String payload = new String(message.getBody());
        return getTradeExecutedMessage(payload);
    }

    private TradeExecutedMessage getTradeExecutedMessage(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, TradeExecutedMessage.class);
    }
}
