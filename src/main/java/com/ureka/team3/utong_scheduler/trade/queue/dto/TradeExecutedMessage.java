package com.ureka.team3.utong_scheduler.trade.queue.dto;


import com.ureka.team3.utong_scheduler.trade.RequestType;
import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TradeExecutedMessage {
    private String dataCode;
    private String requestOrderId;
    private RequestType requestType;
    private Long remain;
    private Long requestPrice;
    private List<TradeMatch> matchedList;
    private List<ContractDto> newContracts;
}
