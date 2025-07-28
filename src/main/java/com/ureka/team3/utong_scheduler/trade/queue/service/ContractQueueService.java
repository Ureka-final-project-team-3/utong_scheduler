package com.ureka.team3.utong_scheduler.trade.queue.service;

import com.ureka.team3.utong_scheduler.trade.queue.dto.AllDataContractDto;
import com.ureka.team3.utong_scheduler.trade.queue.dto.ContractDto;

import java.util.List;

public interface ContractQueueService {

    void initAllRecentContracts();

    void saveContractCacheByDataCode(String dataCode);

//    List<ContractDto> getRecentContracts(String dataCode);

//    AllDataContractDto getRecentContracts();

    List<ContractDto> getRecentContracts(String dataCode);

    void addNewContracts(String dataCode, List<ContractDto> newContract);
}
