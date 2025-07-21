package com.ureka.team3.utong_scheduler.trade.global.config;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.common.enums.GroupCode;
import com.ureka.team3.utong_scheduler.common.repository.CodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Getter
public class DataTradePolicy {
    private final CodeRepository codeRepository;
    private List<Code> tradeStatusCodeList;
    private List<Code> dataTypeCodeList;
    public static final int CHART_LIST_SIZE = 8;
    public static final Long SSE_TIMEOUT = 60 * 60 * 1000L;


    @PostConstruct
    void init() {
        tradeStatusCodeList = codeRepository.findByGroupCode(GroupCode.TRADE_STATUS.getCode());
        dataTypeCodeList = codeRepository.findByGroupCode(GroupCode.DATA_TYPE.getCode());
    }
}
