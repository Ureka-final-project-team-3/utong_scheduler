package com.ureka.team3.utong_scheduler.trade.queue.dto;

import com.ureka.team3.utong_scheduler.trade.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeCancelMessage {
    private RequestType requestType;
    private String dataCode;
    private Long price;
    private Long quantity;

    public static TradeCancelMessage saleCancelMessage(String dataCode, Long price, Long quantity) {
        return TradeCancelMessage.builder()
                .requestType(RequestType.SALE)
                .dataCode(dataCode)
                .price(price)
                .quantity(quantity)
                .build();
    }

    public static TradeCancelMessage purchaseCancelMessage(String dataCode, Long price, Long quantity) {
        return TradeCancelMessage.builder()
                .requestType(RequestType.PURCHASE)
                .dataCode(dataCode)
                .price(price)
                .quantity(quantity)
                .build();
    }
}
