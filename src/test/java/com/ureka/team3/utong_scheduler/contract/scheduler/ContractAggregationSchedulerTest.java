package com.ureka.team3.utong_scheduler.contract.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.contract.dto.AggregationStatusMessage;
import com.ureka.team3.utong_scheduler.contract.repository.ContractHourlyAvgPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractAggregationScheduler 테스트")
class ContractAggregationSchedulerTest {

    @Mock
    private ContractHourlyAvgPriceRepository contractHourlyAvgPriceRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContractAggregationScheduler contractAggregationScheduler;

    private LocalDateTime mockNow;
    private LocalDateTime expectedCurrentHour;
    private LocalDateTime expectedPreviousHour;

    @BeforeEach
    void setUp() {
        // 2025-07-17 10:30:45 라고 가정
        mockNow = LocalDateTime.of(2025, 7, 17, 10, 30, 45);
        expectedCurrentHour = LocalDateTime.of(2025, 7, 17, 10, 0, 0);
        expectedPreviousHour = LocalDateTime.of(2025, 7, 17, 9, 0, 0);
    }

    @Test
    @DisplayName("스케줄러 정상 실행 시 성공 메시지 발행")
    void aggregateHourlyAvgPrice_Success() throws JsonProcessingException {
        // given
        String expectedJson = "{\"status\":\"SUCCESS\"}";

        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        // 1. Repository 메서드 호출 확인
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class));

        // 2. ObjectMapper 호출 확인 (SUCCESS 메시지)
        verify(objectMapper, times(1))
                .writeValueAsString(argThat(message ->
                        message instanceof AggregationStatusMessage &&
                                "SUCCESS".equals(((AggregationStatusMessage) message).getStatus())
                ));

        // 3. Redis 메시지 발행 확인
        verify(stringRedisTemplate, times(1))
                .convertAndSend(eq("contract:aggregation:status"), eq(expectedJson));
    }

    @Test
    @DisplayName("Repository에서 예외 발생 시 실패 메시지 발행")
    void aggregateHourlyAvgPrice_RepositoryException() throws JsonProcessingException {
        // given
        String errorMessage = "Database connection failed";
        RuntimeException repositoryException = new RuntimeException(errorMessage);
        String expectedJson = "{\"status\":\"FAILED\"}";

        doThrow(repositoryException)
                .when(contractHourlyAvgPriceRepository)
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class));

        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        // 1. Repository 메서드 호출 확인
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class));

        // 2. ObjectMapper 호출 확인 (FAILED 메시지)
        verify(objectMapper, times(1))
                .writeValueAsString(argThat(message ->
                        message instanceof AggregationStatusMessage &&
                                "FAILED".equals(((AggregationStatusMessage) message).getStatus())
                ));

        // 3. Redis 실패 메시지 발행 확인
        verify(stringRedisTemplate, times(1))
                .convertAndSend(eq("contract:aggregation:status"), eq(expectedJson));
    }

    @Test
    @DisplayName("Redis 메시지 발행 실패 시에도 예외 전파하지 않음")
    void aggregateHourlyAvgPrice_RedisPublishFailed() throws JsonProcessingException {
        // given
        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenThrow(new JsonProcessingException("JSON serialization failed") {});

        // when & then (예외가 발생하지 않아야 함)
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // Repository 메서드는 정상 호출되어야 함
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("올바른 시간 계산 확인")
    void aggregateHourlyAvgPrice_CorrectTimeCalculation() {
        // given
        // Mock LocalDateTime.now() - ReflectionTestUtils 사용하거나 별도 TimeProvider 필요
        // 여기서는 메서드 호출 시 전달되는 파라미터로 검증

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(contractHourlyAvgPriceRepository).insertHourlyAvgPrice(
                argThat(previousHour ->
                        previousHour.getMinute() == 0 &&
                                previousHour.getSecond() == 0 &&
                                previousHour.getNano() == 0
                ),
                argThat(currentHour ->
                        currentHour.getMinute() == 0 &&
                                currentHour.getSecond() == 0 &&
                                currentHour.getNano() == 0
                )
        );
    }

    @Test
    @DisplayName("성공 메시지 내용 검증")
    void publishAggregationComplete_MessageContent() throws Exception {
        // given
        LocalDateTime testTime = LocalDateTime.of(2025, 7, 17, 9, 0, 0);
        String expectedJson = "{\"status\":\"SUCCESS\"}";

        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        // private 메서드 테스트를 위해 리플렉션 사용
        ReflectionTestUtils.invokeMethod(
                contractAggregationScheduler,
                "publishAggregationComplete",
                testTime
        );

        // then
        verify(objectMapper).writeValueAsString(argThat(message -> {
            AggregationStatusMessage msg = (AggregationStatusMessage) message;
            return "SUCCESS".equals(msg.getStatus()) &&
                    testTime.equals(msg.getAggregatedAt()) &&
                    "계약 평균가 집계 완료".equals(msg.getMessage()) &&
                    msg.getPublishedAt() != null;
        }));

        verify(stringRedisTemplate).convertAndSend(
                eq("contract:aggregation:status"),
                eq(expectedJson)
        );
    }

    @Test
    @DisplayName("실패 메시지 내용 검증")
    void publishAggregationFailed_MessageContent() throws Exception {
        // given
        String errorMessage = "Test error message";
        String expectedJson = "{\"status\":\"FAILED\"}";

        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        ReflectionTestUtils.invokeMethod(
                contractAggregationScheduler,
                "publishAggregationFailed",
                errorMessage
        );

        // then
        verify(objectMapper).writeValueAsString(argThat(message -> {
            AggregationStatusMessage msg = (AggregationStatusMessage) message;
            return "FAILED".equals(msg.getStatus()) &&
                    msg.getAggregatedAt() == null &&
                    msg.getMessage().contains("계약 평균가 집계 실패") &&
                    msg.getMessage().contains(errorMessage) &&
                    msg.getPublishedAt() != null;
        }));

        verify(stringRedisTemplate).convertAndSend(
                eq("contract:aggregation:status"),
                eq(expectedJson)
        );
    }
}