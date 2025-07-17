package com.ureka.team3.utong_scheduler.contract.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_hourly_avg_price")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractHourlyAvgPrice {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "aggregated_at")
    private LocalDateTime aggregatedAt;

    @Column(name = "avg_price")
    private Long avgPrice;
}
