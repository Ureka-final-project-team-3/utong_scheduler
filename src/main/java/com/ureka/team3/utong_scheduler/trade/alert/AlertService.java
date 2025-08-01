package com.ureka.team3.utong_scheduler.trade.alert;

import com.ureka.team3.utong_scheduler.trade.RequestType;
import com.ureka.team3.utong_scheduler.trade.queue.dto.TradeExecutedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    public AlertMessage buildAlertMessage(TradeExecutedMessage message) {
        Map<String, List<ContractAlertDto>> alertMap = new HashMap<>();
        List<ContractDto> contracts = message.getNewContracts();

        if (contracts == null || contracts.isEmpty()) {
            log.warn("계약 정보가 없습니다.");
            return emptyAlertMessage();
        }

        RequestType requestType = message.getRequestType();
        long totalQuantity = 0L;
        LocalDateTime contractedAt = contracts.get(0).getContractedAt();

        if (requestType == RequestType.PURCHASE) {
            String purchaseAccountId = contracts.get(0).getPurchaseAccountId();
            Map<Long, Long> priceToQuantityMap = new HashMap<>();

            for (ContractDto contract : contracts) {
                totalQuantity += contract.getQuantity();

                // 판매자 알림
                ContractAlertDto sellerAlert = createAlertDto(
                        RequestType.SALE,
                        contract,
                        contract.getSaleOrderId()
                );
                alertMap.computeIfAbsent(contract.getSaleAccountId(), k -> new ArrayList<>())
                        .add(sellerAlert);

                // 구매자 가격별 수량 집계
                priceToQuantityMap.merge(contract.getPrice(), contract.getQuantity(), Long::sum);
            }

            // 구매자 알림 (가격별)
            List<ContractAlertDto> buyerAlerts = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : priceToQuantityMap.entrySet()) {
                ContractAlertDto buyerAlert = createAlertDto(
                        RequestType.PURCHASE,
                        message.getDataCode(),
                        entry.getValue(),
                        contractedAt,
                        entry.getKey(),
                        message.getRequestOrderId()
                );
                buyerAlerts.add(buyerAlert);
            }
            alertMap.put(purchaseAccountId, buyerAlerts);

        } else if (requestType == RequestType.SALE) {
            String saleAccountId = contracts.get(0).getSaleAccountId();
            Map<Long, Long> priceToQuantityMap = new HashMap<>();

            for (ContractDto contract : contracts) {
                totalQuantity += contract.getQuantity();

                // 구매자 알림
                ContractAlertDto buyerAlert = createAlertDto(
                        RequestType.PURCHASE,
                        contract,
                        contract.getPurchaseOrderId()
                );
                alertMap.computeIfAbsent(contract.getPurchaseAccountId(), k -> new ArrayList<>())
                        .add(buyerAlert);

                // 판매자 가격별 수량 집계
                priceToQuantityMap.merge(contract.getPrice(), contract.getQuantity(), Long::sum);
            }

            // 판매자 알림 (가격별)
            List<ContractAlertDto> sellerAlerts = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : priceToQuantityMap.entrySet()) {
                ContractAlertDto sellerAlert = createAlertDto(
                        RequestType.SALE,
                        message.getDataCode(),
                        entry.getValue(),
                        contractedAt,
                        entry.getKey(),
                        message.getRequestOrderId()
                );
                sellerAlerts.add(sellerAlert);
            }
            alertMap.put(saleAccountId, sellerAlerts);
        }

        return AlertMessage.builder()
                .publishedAt(LocalDateTime.now())
                .dataMap(alertMap)
                .build();
    }

    private static ContractAlertDto createAlertDto(RequestType type, ContractDto contract, String orderId) {
        return ContractAlertDto.builder()
                .requestType(type)
                .dataCode(contract.getDataCode())
                .quantity(contract.getQuantity())
                .contractedAt(contract.getContractedAt())
                .price(contract.getPrice())
                .orderId(orderId)
                .build();
    }

    private static ContractAlertDto createAlertDto(RequestType type, String dataCode, long quantity,
                                                   LocalDateTime contractedAt, long price, String orderId) {
        return ContractAlertDto.builder()
                .requestType(type)
                .dataCode(dataCode)
                .quantity(quantity)
                .contractedAt(contractedAt)
                .price(price)
                .orderId(orderId)
                .build();
    }

    private static AlertMessage emptyAlertMessage() {
        return AlertMessage.builder()
                .publishedAt(LocalDateTime.now())
                .dataMap(new HashMap<>())
                .build();
    }
}
