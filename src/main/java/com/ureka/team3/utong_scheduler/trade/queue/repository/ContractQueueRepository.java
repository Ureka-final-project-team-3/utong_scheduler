package com.ureka.team3.utong_scheduler.trade.queue.repository;

import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;

import java.util.List;

public interface ContractQueueRepository {

    void saveBatchContracts(String dataCode, List<ContractDto> contracts);

    void saveNewContract(String dataCode, ContractDto contract); // 신규 추가

    List<ContractDto> getAllCachedContracts(String dataCode);

}
