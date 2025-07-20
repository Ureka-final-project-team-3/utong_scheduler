package com.ureka.team3.utong_scheduler.contract.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.contract.dto.AvgPerHour;
import com.ureka.team3.utong_scheduler.contract.utils.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ContractHourlyAvgPriceRedisRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;


    public void rightPushAll(String dataCode, List<AvgPerHour> avgPerHours) {
        try {
            String key = RedisKeyUtil.buildCurrentPriceListKey(dataCode);

//            Collections.sort(avgPerHours, (a, b) -> a.getAggregatedAt().compareTo(b.getAggregatedAt()));

            List<String> jsonValues = avgPerHours.stream()
                    .map(this::convertToJson)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!jsonValues.isEmpty()) {
                stringRedisTemplate.opsForList().rightPushAll(key, jsonValues);
            }

            log.info("Redis 평균가 데이터 저장 완료 - dataCode: {}, count: {}", dataCode, avgPerHours.size());
        } catch (Exception e) {
            log.error("Redis 평균가 데이터 저장 실패 - dataCode: {}, error: {}", dataCode, e.getMessage());
        }
    }

    public void addDataWithSizeLimit(String dataCode, AvgPerHour avgPerHour, int sizeLimit) {
        try {
            String key = RedisKeyUtil.buildCurrentPriceListKey(dataCode);
            Long currentSize = stringRedisTemplate.opsForList().size(key);

            if (currentSize != null && currentSize >= sizeLimit) {
                String s = stringRedisTemplate.opsForList().leftPop(key);// 가장 오래된 데이터 제거
                log.info("제거 값 - {}", s);
            }


            stringRedisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(avgPerHour));

            log.info("Redis 평균가 데이터 추가 완료 - dataCode: {}, avgPrice: {}, aggregatedAt: {}",
                    dataCode, avgPerHour.getAvgPrice(), avgPerHour.getAggregatedAt());
        } catch (Exception e) {
            log.error("Redis 평균가 데이터 추가 실패 - dataCode: {}, error: {}", dataCode, e.getMessage());
        }
    }

    public void initializeData(String dataCode, List<AvgPerHour> initialData) {
        try {
            String key = RedisKeyUtil.buildCurrentPriceListKey(dataCode);
            stringRedisTemplate.delete(key); // 기존 데이터 삭제

            if (!initialData.isEmpty()) {
                rightPushAll(dataCode, initialData);
            }

            log.info("Redis 초기 평균가 데이터 저장 완료 - dataCode: {}, count: {}", dataCode, initialData.size());
        } catch (Exception e) {
            log.error("Redis 초기 평균가 데이터 저장 실패 - dataCode: {}, error: {}", dataCode, e.getMessage());
        }
    }

    public List<AvgPerHour> getAllData(String dataCode) {
        try {
            String key = RedisKeyUtil.buildCurrentPriceListKey(dataCode);
            List<String> jsonList = stringRedisTemplate.opsForList().range(key, 0, -1);

            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            return jsonList.stream()
                    .map(this::convertFromJson)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Redis 평균가 데이터 조회 실패 - dataCode: {}, error: {}", dataCode, e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean isEmpty(String dataCode) {
        try {
            String key = RedisKeyUtil.buildCurrentPriceListKey(dataCode);
            Long size = stringRedisTemplate.opsForList().size(key);
            return size == null || size == 0;
        } catch (Exception e) {
            log.error("Redis List 크기 확인 실패 - dataCode: {}, error: {}", dataCode, e.getMessage());
            return true;
        }
    }

    private String convertToJson(AvgPerHour avgPerHour) {
        try {
            return objectMapper.writeValueAsString(avgPerHour);
        } catch (Exception e) {
            log.error("JSON 직렬화 실패: {}", avgPerHour, e);
            return null;
        }
    }

    private AvgPerHour convertFromJson(String json) {
        try {
            return objectMapper.readValue(json, AvgPerHour.class);
        } catch (Exception e) {
            log.error("JSON 역직렬화 실패: {}", json, e);
            return null;
        }
    }
}
