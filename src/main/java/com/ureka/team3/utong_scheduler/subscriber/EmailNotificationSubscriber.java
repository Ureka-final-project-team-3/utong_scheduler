package com.ureka.team3.utong_scheduler.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.auth.entity.Account;
import com.ureka.team3.utong_scheduler.auth.service.AccountService;
import com.ureka.team3.utong_scheduler.config.RabbitMQConfig;
import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import com.ureka.team3.utong_scheduler.trade.notification.enums.ContractType;
import com.ureka.team3.utong_scheduler.trade.notification.service.TradeNotificationService;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeExecutedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationSubscriber {

    private final AccountService accountService;
    private final TradeNotificationService tradeNotificationService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailNotification(TradeExecutedMessage message) {
        log.info("이메일 발송 메시지 수신 - 데이터코드 : {}, 계약 수 : {}",
                message.getDataCode(),
                message.getNewContracts().size());

        try {
            sendContractNotificationEmails(message);
        } catch (Exception e) {
            log.error("이메일 발송 처리 실패 - 에러 : {}", e.getMessage());
            throw e;
        }
    }

    public void sendContractNotificationEmails(TradeExecutedMessage message) {
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
}
