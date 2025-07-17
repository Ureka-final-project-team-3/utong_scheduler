package com.ureka.team3.utong_scheduler.contract.entity;

import com.ureka.team3.utong_scheduler.auth.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buy_data_request")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyDataRequest {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "FK_account_TO_buy_data_request_1"))
    private Account account;

    private Long price;

    @Column(name = "data_code", length = 3)
    private String dataCode;

    private Long quantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "line_id")
    private String lineId;

    @Column
    private String status;

    @PrePersist
    public void initId() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.expiredAt == null) this.expiredAt = createdAt.plusDays(3);
    }

    public void changeStatus(String status) {
        this.status = status;
    }
}
