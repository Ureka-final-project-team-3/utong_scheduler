package com.ureka.team3.utong_scheduler.trade.queue.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class OrderDto {
    private String orderId;     // 주문 ID
    private long quantity;      // 데이터 용량 (GB 등)
    private long createdAt;
    private long expiredAt;
    private long price;
    private String dataCode;
}
