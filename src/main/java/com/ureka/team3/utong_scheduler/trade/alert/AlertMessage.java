package com.ureka.team3.utong_scheduler.trade.alert;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AlertMessage {
    private LocalDateTime publishedAt;
    private Map<String, List<ContractAlertDto>> dataMap;
}
