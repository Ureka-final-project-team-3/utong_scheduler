package com.ureka.team3.utong_scheduler.trade.global.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_data_request_id", nullable = false)
    private SaleDataRequest saleDataRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_data_request_id", nullable = false)
    private BuyDataRequest buyDataRequest;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column
    private Long price;

    @Column
    private Long amount;

    // 판매수량, 개당 판매금액 필요해 보임
    @PrePersist
    public void initId() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
