package com.ureka.team3.utong_scheduler.trade.queue.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.global.utils.RedisKeyUtil;
import com.ureka.team3.utong_scheduler.trade.alert.ContractDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ContractQueueRepositoryImpl implements ContractQueueRepository{

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void saveBatchContracts(String dataCode, List<ContractDto> contracts) {
        try {
            String key = RedisKeyUtil.buildContractListKey(dataCode);

            stringRedisTemplate.delete(key);

            if(contracts.isEmpty()) {
                log.info("초기화할 계약 데이터가 없습니다. dataCode: {}", dataCode);
                return;
            }

            List<String> contractJsonList = new ArrayList<>();

            for(int i = contracts.size() - 1; i >= 0; i--) {
                ContractDto contract = contracts.get(i);
                String contractJson = objectMapper.writeValueAsString(contract);
                contractJsonList.add(contractJson);
            }

            if (!contractJsonList.isEmpty()) {
                stringRedisTemplate.opsForList().leftPushAll(key, contractJsonList.toArray(new String[0]));
            }

            Long finalSize = stringRedisTemplate.opsForList().size(key);

            log.info("배치 캐시 초기화 완료 - dataCode: {}, 입력: {}건, 최종크기: {}건",
                    dataCode, contracts.size(), finalSize);
        } catch (Exception e) {
            log.error("배치 캐시 초기화 실패 - dataCode: {}, error: {}", dataCode, e.getMessage(), e);
        }
    }

    @Override
    public void saveNewContract(String dataCode, ContractDto contract) {
        String redisKey = RedisKeyUtil.buildContractListKey(dataCode);
        String serialized = serialize(contract);
        stringRedisTemplate.opsForList().leftPush(redisKey, serialized);
        stringRedisTemplate.opsForList().trim(redisKey, 0, DataTradePolicy.CONTRACT_LIST_SIZE - 1);
    }

    @Override
    public List<ContractDto> getAllCachedContracts(String dataCode) {
        try {
            String key = RedisKeyUtil.buildContractListKey(dataCode);

            List<String> contractJsonList = stringRedisTemplate.opsForList().range(key, 0, -1);

            if (contractJsonList == null || contractJsonList.isEmpty()) {
                log.info("캐시된 계약 데이터가 없습니다. dataCode: {}", dataCode);
                return new ArrayList<>();
            }

            List<ContractDto> contracts = new ArrayList<>();

            for (String json : contractJsonList) {
                try {
                    ContractDto contract = objectMapper.readValue(json, ContractDto.class);
                    contracts.add(contract);
                } catch (JsonProcessingException e) {
                    log.error("계약 데이터 파싱 실패 - dataCode: {}, json: {}, error: {}", dataCode, json, e.getMessage());
                }
            }

            log.info("캐시된 계약 데이터 조회 완료 - dataCode: {}, size: {}", dataCode, contracts.size());
            return contracts;
        } catch (Exception e) {
            log.error("캐시된 계약 데이터 조회 실패 - dataCode: {}, error: {}", dataCode, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String serialize(ContractDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
