package com.ureka.team3.utong_scheduler.subscriber;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.auth.entity.Account;
import com.ureka.team3.utong_scheduler.auth.service.AccountService;
import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.publisher.AlertPublisher;
import com.ureka.team3.utong_scheduler.publisher.TradeQueuePublisher;
import com.ureka.team3.utong_scheduler.trade.RequestType;
import com.ureka.team3.utong_scheduler.trade.alert.AlertService;
import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.global.entity.Contract;
import com.ureka.team3.utong_scheduler.trade.notification.enums.ContractType;
import com.ureka.team3.utong_scheduler.trade.notification.service.TradeNotificationService;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeExecutedMessage;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeMatch;
import com.ureka.team3.utong_scheduler.trade.queue.service.ContractQueueService;
import com.ureka.team3.utong_scheduler.trade.queue.service.TradeQueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExecutedSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final TradeQueueService tradeQueueService;
    private final TradeQueuePublisher tradeQueuePublisher;
    private final ContractQueueService contractQueueService;
    private final DataTradePolicy dataTradePolicy;
    private final AlertService alertService;
    private final AlertPublisher alertPublisher;
    private final AccountService accountService;
    private final TradeNotificationService tradeNotificationService;
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            log.info("집계 완료 메시지 수신: {}", message);
            TradeExecutedMessage tradeExecutedMessage = getTradeExecutedMessage(message);
            Map<Long, Long> saleDataChanges = new HashMap<>();
            Map<Long, Long> purchaseDataChanges = new HashMap<>();
            List<TradeMatch> matchedList = tradeExecutedMessage.getMatchedList();
            if (tradeExecutedMessage.getRequestType().equals(RequestType.PURCHASE)) {
                purchaseDataChanges.put(tradeExecutedMessage.getRequestPrice(), tradeExecutedMessage.getRemain());
                if(matchedList!=null&&!matchedList.isEmpty()){
                    for (TradeMatch tradeMatch : matchedList) {
                        Long pricePerUnit = tradeMatch.getPricePerUnit();
                        Long amount = tradeMatch.getAmount();

                        saleDataChanges.put(pricePerUnit,
                                saleDataChanges.getOrDefault(pricePerUnit, 0L) - amount);
                    }
                }

            } else {
                saleDataChanges.put(tradeExecutedMessage.getRequestPrice(), tradeExecutedMessage.getRemain());
                if(matchedList!=null&&!matchedList.isEmpty()){
                    for (TradeMatch tradeMatch : matchedList) {
                        Long pricePerUnit = tradeMatch.getPricePerUnit();
                        Long amount = tradeMatch.getAmount();

                        purchaseDataChanges.put(pricePerUnit,
                                purchaseDataChanges.getOrDefault(pricePerUnit, 0L) - amount);
                    }
                }

            }

            tradeQueueService.changeCurrentDataAmount(tradeExecutedMessage.getDataCode(), saleDataChanges, purchaseDataChanges);
            if (tradeExecutedMessage.getNewContracts() != null && !tradeExecutedMessage.getNewContracts().isEmpty()) {
                contractQueueService.addNewContracts(tradeExecutedMessage.getDataCode(), tradeExecutedMessage.getNewContracts());
                alertPublisher.publish(LocalDateTime.now(), alertService.buildAlertMessage(tradeExecutedMessage));
                sendContractNotificationEmails(tradeExecutedMessage);
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
	    } catch(
	    Exception e)
	
	    {
	        tradeQueueService.init();
	        log.error("집계 완료 메시지 처리 중 오류: {}", e.getMessage(), e);
	    }
	}

    private void sendContractNotificationEmails(TradeExecutedMessage message) {
        try {
            Set<String> processedAccounts = new HashSet<>();
            List<ContractDto> contracts = message.getNewContracts();
            
            if (contracts == null || contracts.isEmpty()) {
                return;
            }

            log.info("거래 체결 메일 발송 시작 - 계약 수: {}", contracts.size());

            for (ContractDto contract : contracts) {
                sendEmailToAccount(contract.getPurchaseAccountId(), ContractType.BUY, processedAccounts, contract);
                sendEmailToAccount(contract.getSaleAccountId(), ContractType.SALE, processedAccounts, contract);
            }

            log.info("거래 체결 메일 발송 완료 - 발송 계정 수: {}", processedAccounts.size());
            
        } catch (Exception e) {
            log.error("거래 체결 메일 발송 중 오류: {}", e.getMessage(), e);
        }
    }
    private void sendEmailToAccount(String accountId, ContractType contractType, Set<String> processedAccounts, ContractDto contractDto) {
        if (accountId == null || processedAccounts.contains(accountId)) {
            return; 
        }

        try {
			Account account = accountService.findById(accountId);
            
            if (account == null || account.getEmail() == null) {
                log.warn("계정 정보 또는 이메일이 없습니다. accountId: {}", accountId);
                return;
            }
            boolean success = tradeNotificationService.sendContractCompleteMessage(
                account.getEmail(), account.getNickname(), contractType, contractDto
            );
            
            if (success) {
                log.info("거래 체결 메일 발송 성공 - 이메일: {}, 타입: {}", account.getEmail(), contractType);
            } else {
                log.warn("거래 체결 메일 발송 실패 - 이메일: {}, 타입: {}", account.getEmail(), contractType);
            }
            
            processedAccounts.add(accountId);
            
        } catch (Exception e) {
            log.error("계정 {}에게 메일 발송 실패: {}", accountId, e.getMessage());
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
