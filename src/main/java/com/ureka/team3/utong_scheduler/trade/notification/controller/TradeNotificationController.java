package com.ureka.team3.utong_scheduler.trade.notification.controller;

import com.ureka.team3.utong_scheduler.trade.notification.enums.ContractType;
import com.ureka.team3.utong_scheduler.trade.notification.service.TradeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade")
@Slf4j
public class TradeNotificationController {

    private final TradeNotificationService tradeNotificationService;

    @PostMapping("/notification/contract-complete")
    public ResponseEntity<String> sendContractCompleteNotification(
            @RequestParam("to") String to,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam("contractType") String contractType
    ) {
        log.info("HTML 메일 요청 - 수신자: {}, 사용자명: {}, 계약 유형: {}", to, nickname, contractType);

        boolean success = tradeNotificationService.sendContractCompleteMessage(
                to,
                nickname,
                contractType.equals("BUY") ? ContractType.BUY : ContractType.SALE
        );

        if(success)
            return ResponseEntity.ok("계약 완료 알림 메일이 전송되었습니다.");
        else
            return ResponseEntity.status(500).body("계약 완료 알림 메일 전송에 실패했습니다.");
    }
}
