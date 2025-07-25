package com.ureka.team3.utong_scheduler.trade.queue.service;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.queue.dto.ContractDto;
import com.ureka.team3.utong_scheduler.trade.queue.repository.ContractQueueRepository;
import com.ureka.team3.utong_scheduler.trade.queue.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractQueueServiceImpl implements ContractQueueService {

    private final DataTradePolicy dataTradePolicy;
    private final ContractRepository contractRepository;
    private final ContractQueueRepository contractQueueRepository;

    @Override
    public void initAllRecentContracts() {
        log.info("Contract Queue 캐시 초기화: 최근 {}개 계약 Redis 저장", DataTradePolicy.CONTRACT_LIST_SIZE);

        List<String> dataTypeCodes = dataTradePolicy.getDataTypeCodeList()
                .stream()
                .map(Code::getCode)
                .toList();

        for (Code code : dataTradePolicy.getDataTypeCodeList()) {
            String dataCode = code.getCode();
            log.info("저장 시작 - dataCode: {}", dataCode);
            saveContractCacheByDataCode(dataCode);
        }
    }


    @Override
    public void saveContractCacheByDataCode(String dataCode) {
        try {
            log.info("계약 캐시 저장 시작 - dataCode: {}", dataCode);

            List<ContractDto> recentContracts
                    = contractRepository.findLatestContractByDataCode(dataCode, DataTradePolicy.CONTRACT_LIST_SIZE)
                    .stream()
                    .map(contract -> ContractDto.of(contract, dataCode))
                    .toList();

            if (recentContracts.isEmpty()) {
                log.info("저장할 계약 데이터가 없습니다. dataCode: {}", dataCode);

                contractQueueRepository.saveBatchContracts(dataCode, recentContracts);
                return;
            }

            contractQueueRepository.saveBatchContracts(dataCode, recentContracts);

            log.info("계약 캐시 저장 완료 - dataCode: {}, 저장된 계약 수: {}", dataCode, recentContracts.size());
        } catch (Exception e) {
            log.error("계약 캐시 저장 중 오류 발생 - dataCode: {}, error: {}", dataCode, e.getMessage(), e);
        }
    }

    @Override
    public List<ContractDto> getRecentContracts(String dataCode) {
        try {
            return contractQueueRepository.getAllCachedContracts(dataCode);
        } catch (Exception e) {
            log.error("계약 캐시 조회 중 오류 발생 -  error: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void addNewContracts(String dataCode, List<ContractDto> newContracts) {
        if (newContracts == null || newContracts.isEmpty()) return;

        newContracts.forEach(contractDto -> {
            try {
                log.debug("새로운 계약 캐시에 추가 - dataCode: {}", dataCode);
                contractQueueRepository.saveNewContract(dataCode, contractDto);
            } catch (Exception e) {
                log.error("신규 계약 저장 중 오류 발생 - dataCode: {}, error: {}", dataCode, e.getMessage(), e);
            }
        });
    }

}
