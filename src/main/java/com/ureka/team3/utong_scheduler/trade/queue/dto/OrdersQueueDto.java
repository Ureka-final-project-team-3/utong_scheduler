package com.ureka.team3.utong_scheduler.trade.queue.dto;

import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdersQueueDto {
    private Map<Long,Long> buyOrderQuantity;
    private Map<Long,Long> sellOrderQuantity;
    private List<ContractDto> recentContracts;
}
