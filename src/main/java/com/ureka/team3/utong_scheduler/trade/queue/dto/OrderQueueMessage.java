package com.ureka.team3.utong_scheduler.trade.queue.dto;

import com.ureka.team3.utong_scheduler.trade.chart.dto.AvgPerHour;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderQueueMessage {
    private String status;

    private LocalDateTime publishedAt;

    private String message;

    private Map<String, OrdersQueueDto> dataMap;

}
