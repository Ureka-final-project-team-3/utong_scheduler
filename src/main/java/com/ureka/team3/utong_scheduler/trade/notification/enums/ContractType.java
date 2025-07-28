package com.ureka.team3.utong_scheduler.trade.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractType {
    BUY("구매"),
    SALE("판매");

    private final String description;

}
