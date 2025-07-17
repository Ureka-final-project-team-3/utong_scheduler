package com.ureka.team3.utong_scheduler.contract.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregationStatusMessage {

    private String status;

    private LocalDateTime aggregatedAt;

    private LocalDateTime publishedAt;

    private String message;

}
