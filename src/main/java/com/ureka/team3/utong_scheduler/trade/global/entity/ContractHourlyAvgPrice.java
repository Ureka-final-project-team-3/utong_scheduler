package com.ureka.team3.utong_scheduler.trade.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_hourly_avg_price")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractHourlyAvgPrice {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "aggregated_at")
    private LocalDateTime aggregatedAt;

    @Column(name = "avg_price")
    private Long avgPrice;

    @Column(name = "data_code", length = 3)
    String dataCode;
}
