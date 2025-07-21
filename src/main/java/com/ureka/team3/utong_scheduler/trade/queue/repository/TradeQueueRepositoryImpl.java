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

        Map<String, String> hashMap = buyOrderQuantity.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));

        stringRedisTemplate.opsForHash().putAll(key, hashMap);
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
