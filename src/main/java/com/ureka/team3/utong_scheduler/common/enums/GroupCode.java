package com.ureka.team3.utong_scheduler.common.enums;

import java.util.Arrays;

public enum GroupCode {
    TRADE_STATUS("010", "거래 상태"),
    DATA_TYPE("020", "데이터 종류"),
    REWARD_TYPE("030", "쿠폰 타입"),
    AVAILABILITY("040", "사용 가능 상태"),
    PROCESS_STATUS("060", "진행 상태"),
    TRADE_DIRECTION("070", "거래 방향");

    private final String code;
    private final String description;

    GroupCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GroupCode from(String code) {
        return Arrays.stream(values())
                .filter(g -> g.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown group code: " + code));
    }
}
