package com.ureka.team3.utong_scheduler.trade.alert;

import com.ureka.team3.utong_scheduler.trade.RequestType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContractAlertDto{
        RequestType requestType;
        Long price;
        Long quantity;
        String dataCode;
        String orderId;
        LocalDateTime contractedAt;
}
