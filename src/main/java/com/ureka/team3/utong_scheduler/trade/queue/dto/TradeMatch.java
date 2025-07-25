package com.ureka.team3.utong_scheduler.trade.queue.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeMatch {
    private OrderDto matchedOrder;
    private Long amount;
    private Long pricePerUnit;

    public static TradeMatch of(OrderDto order, long amount) {
        return TradeMatch.builder()
                .matchedOrder(order)
                .pricePerUnit(order.getPrice())
                .amount(amount)
                .build();
    }
}