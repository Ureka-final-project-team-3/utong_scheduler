package com.ureka.team3.utong_scheduler.price.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "price")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Price {

    @Id
    private String id;

    private Long minimumPrice;

    private Float minimumRate;

    private Float tax;

}
