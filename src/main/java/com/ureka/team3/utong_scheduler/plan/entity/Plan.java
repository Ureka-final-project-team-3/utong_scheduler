package com.ureka.team3.utong_scheduler.plan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "data")
    private Long data;

    @Column(name = "available_trade_rate")
    private float availableTradeRate;

    @Column(name = "data_code", length = 3)
    private String dataCode;
}