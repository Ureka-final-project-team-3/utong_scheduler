package com.ureka.team3.utong_scheduler.trade.queue.dto;

import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AllDataContractDto {
    private Map<String,List<ContractDto>> contractMap;
}
