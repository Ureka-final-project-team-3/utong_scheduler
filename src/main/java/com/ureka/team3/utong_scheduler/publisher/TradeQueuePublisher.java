package com.ureka.team3.utong_scheduler.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.trade.chart.dto.AggregationStatusMessage;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrderQueueMessage;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeQueuePublisher implements RedisPublisher<Map<String, OrdersQueueDto>> {
    private static final String QUEUE_AGGREGATION_STATUS_CHANNEL = "queue:aggregation:status";
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(LocalDateTime localDateTime, Map<String, OrdersQueueDto> data) {
        try {
            OrderQueueMessage message = OrderQueueMessage.builder()
                    .status("SUCCESS")
                    .publishedAt(LocalDateTime.now())
                    .message("거래 대기열 집계 완료")
                    .dataMap(data)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(QUEUE_AGGREGATION_STATUS_CHANNEL, jsonMessage);
            log.info("대기열 전송 완료");
        } catch (Exception e) {
            log.error("집계 완료 메시지 발행 중 오류 발생: {}", e.getMessage());
        }
    }

    @Override
    public void noticeFailed(String errorMessage) {
        try {
            AggregationStatusMessage message = AggregationStatusMessage.builder()
                    .status("FAILED")
                    .aggregatedAt(null)
                    .publishedAt(LocalDateTime.now())
                    .message("거래 대기열 집계 완료" + errorMessage)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(QUEUE_AGGREGATION_STATUS_CHANNEL, jsonMessage);

            log.error("집계 실패 메시지 발행: {}", errorMessage);
        } catch (Exception e) {
            log.error("집계 실패 메시지 발행 중 오류 발생: {}", e.getMessage());
        }
    }
}
