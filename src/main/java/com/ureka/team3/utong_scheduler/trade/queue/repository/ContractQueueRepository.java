package com.ureka.team3.utong_scheduler.trade.queue.repository;

import com.ureka.team3.utong_scheduler.trade.queue.dto.ContractDto;

import java.util.List;

public interface ContractQueueRepository {

    void saveBatchContracts(String dataCode, List<ContractDto> contracts);

    List<ContractDto> getAllCachedContracts(String dataCode);

}
