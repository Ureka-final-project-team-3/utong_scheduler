package com.ureka.team3.utong_scheduler.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.publisher.AlertPublisher;
import com.ureka.team3.utong_scheduler.publisher.TradeQueuePublisher;
import com.ureka.team3.utong_scheduler.trade.RequestType;
import com.ureka.team3.utong_scheduler.trade.alert.AlertService;
import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeCancelMessage;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeExecutedMessage;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeMatch;
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
public class TradeCanceledSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final TradeQueueService tradeQueueService;
    private final TradeQueuePublisher tradeQueuePublisher;
    private final ContractQueueService contractQueueService;
    private final DataTradePolicy dataTradePolicy;
    private final AlertService alertService;
    private final AlertPublisher alertPublisher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            log.info("취소 완료 메세지 수신: {}", message);
            TradeCancelMessage tradeExecutedMessage = getTradeCanceledMessage(message);
            Map<Long,Long> saleDataChanges = new HashMap<>();
            Map<Long,Long> purchaseDataChanges = new HashMap<>();
            if(tradeExecutedMessage.getRequestType().equals(RequestType.PURCHASE)){
                purchaseDataChanges.put(tradeExecutedMessage.getPrice(),tradeExecutedMessage.getQuantity()*-1);
            }

            if(tradeExecutedMessage.getRequestType().equals(RequestType.SALE)){
                saleDataChanges.put(tradeExecutedMessage.getPrice(),tradeExecutedMessage.getQuantity()*-1);
            }
            tradeQueueService.changeCurrentDataAmount(tradeExecutedMessage.getDataCode(), saleDataChanges, purchaseDataChanges);


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
    } catch(
    Exception e)

    {
        tradeQueueService.init();
        log.error("집계 완료 메시지 처리 중 오류: {}", e.getMessage(), e);
    }
}


private TradeCancelMessage getTradeCanceledMessage(Message message) throws JsonProcessingException {
    String payload = new String(message.getBody());
    return getTradeCanceledMessage(payload);
}

private TradeCancelMessage getTradeCanceledMessage(String payload) throws JsonProcessingException {
    return objectMapper.readValue(payload, TradeCancelMessage.class);
}
}
