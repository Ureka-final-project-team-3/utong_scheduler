package com.ureka.team3.utong_scheduler.trade.queue.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrderDto;
import com.ureka.team3.utong_scheduler.trade.global.utils.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Repository
@RequiredArgsConstructor
public class TradeQueueRepositoryImpl implements TradeQueueRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Map<Long, List<OrderDto>> findAllSellOrders(String dataCode) {
        return findAllOrdersByPattern(RedisKeyUtil.buildCommonSellListKey(dataCode));
    }

    @Override
    public Map<Long, List<OrderDto>> findAllBuyOrders(String dataCode) {
        return findAllOrdersByPattern(RedisKeyUtil.buildCommonBuyListKey(dataCode));
    }

    @Override
    public void saveAllSellOrdersNumber(String dataCode, Map<Long, Long> sellOrderQuantity) {
        String key = "sell:numbers:" + dataCode;

        stringRedisTemplate.delete(key);

        Map<String, String> hashMap = sellOrderQuantity.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));

        stringRedisTemplate.opsForHash().putAll(key, hashMap);
    }

    @Override
    public void saveAllBuyOrdersNumber(String dataCode, Map<Long, Long> buyOrderQuantity) {
        String key = "buy:numbers:" + dataCode;

        // 기존 데이터 초기화
        stringRedisTemplate.delete(key);

        // 저장할 데이터 변환
        Map<String, String> hashMap = buyOrderQuantity.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));

        // 전체 저장
        stringRedisTemplate.opsForHash().putAll(key, hashMap);
    }

    @Override
    public void eraseAllSellOrdersNumber(String dataCode) {
        String key = "sell:numbers:" + dataCode;

        if (stringRedisTemplate.hasKey(key)) {
            stringRedisTemplate.delete(key);
            log.info("모든 판매 주문 수량 삭제 완료: {}", key);
        } else {
            log.warn("판매 주문 수량 키가 존재하지 않음: {}", key);
        }
    }

    @Override
    public void eraseAllBuyOrdersNumber(String dataCode) {
        String key = "buy:numbers:" + dataCode;

        if (stringRedisTemplate.hasKey(key)) {
            stringRedisTemplate.delete(key);
            log.info("모든 구매 주문 수량 삭제 완료: {}", key);
        } else {
            log.warn("구매 주문 수량 키가 존재하지 않음: {}", key);
        }
    }

    @Override
    public void changeCurrentSellOrderQuantity(String dataCode, Long price, Long amount) {
        String key = "sell:numbers:" + dataCode;
        stringRedisTemplate.opsForHash().increment(key, price.toString(), amount);
    }

    @Override
    public void changeCurrentBuyOrderQuantity(String dataCode, Long price, Long amount) {
        String key = "buy:numbers:" + dataCode;
        stringRedisTemplate.opsForHash().increment(key, price.toString(), amount);
    }

    @Override
    public Map<Long, Long> getAllBuyOrderQuantities(String dataCode) {
        String key = "buy:numbers:" + dataCode;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        return entries.entrySet().stream()
                .map(e -> Map.entry(Long.parseLong(e.getKey().toString()), Long.parseLong(e.getValue().toString())))
                .filter(e -> e.getValue() > 0) // 수량이 0 초과인 경우만
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<Long, Long> getAllSellOrderQuantities(String dataCode) {
        String key = "sell:numbers:" + dataCode;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        return entries.entrySet().stream()
                .map(e -> Map.entry(Long.parseLong(e.getKey().toString()), Long.parseLong(e.getValue().toString())))
                .filter(e -> e.getValue() > 0) // 수량이 0 초과인 경우만
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Long, List<OrderDto>> findAllOrdersByPattern(String keyPattern) {
        Set<String> keys = stringRedisTemplate.keys(keyPattern);
        if (keys == null || keys.isEmpty()) return Map.of();

        Map<Long, List<OrderDto>> result = new HashMap<>();

        for (String listKey : keys) {
            List<String> rawOrders = stringRedisTemplate.opsForList().range(listKey, 0, -1);
            if (rawOrders == null || rawOrders.isEmpty()) continue;

            Optional<Long> priceOpt = extractPriceFromKey(listKey);
            if (priceOpt.isEmpty()) continue;

            List<OrderDto> orders = parseOrderList(rawOrders);
            result.put(priceOpt.get(), orders);
        }

        return result;
    }

    private Optional<Long> extractPriceFromKey(String listKey) {
        String[] parts = listKey.split(":");
        if (parts.length != 4) return Optional.empty();

        try {
            return Optional.of(Long.parseLong(parts[3]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private List<OrderDto> parseOrderList(List<String> rawOrders) {
        List<OrderDto> orders = new ArrayList<>();
        for (String json : rawOrders) {
            try {
                orders.add(objectMapper.readValue(json, OrderDto.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON 파싱 실패: " + json, e);
            }
        }
        return orders;
    }


    private long calculateTTL(long expiredAtMillis) {
        long now = System.currentTimeMillis();
        return Math.max(0, (expiredAtMillis - now) / 1000);
    }

    private String toJson(OrderDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 직렬화 실패", e);
        }
    }
}
