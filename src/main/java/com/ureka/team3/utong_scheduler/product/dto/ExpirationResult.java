package com.ureka.team3.utong_scheduler.product.dto;

import com.ureka.team3.utong_scheduler.product.global.enums.ProductType;
import lombok.*;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExpirationResult {

    private int processedCount;

    private long duration;

    private ProductType type;

    private String errorMessage;

}
