package com.ureka.team3.utong_scheduler.contract.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureka.team3.utong_scheduler.contract.dto.AggregationStatusMessage;
import com.ureka.team3.utong_scheduler.contract.repository.ContractHourlyAvgPriceRepository;
import com.ureka.team3.utong_scheduler.price.entity.Price;
import com.ureka.team3.utong_scheduler.price.repository.PriceRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractAggregationScheduler 유닛 테스트")
class ContractAggregationSchedulerTest {

    @Mock
    private ContractHourlyAvgPriceRepository contractHourlyAvgPriceRepository;

    @Mock
    private PriceRepository priceRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContractAggregationScheduler contractAggregationScheduler;

    private final String PRICE_ID = "903ee67c-71b3-432e-bbd1-aaf5d5043376";
    private final String CONTRACT_AGGREGATION_STATUS_CHANNEL = "contract:aggregation:status";

    @BeforeEach
    void setUp() {
        // static final 필드는 리플렉션으로 변경할 수 없으므로 setUp 메서드에서 제거
        // 대신 테스트에서 실제 상수 값을 직접 사용
    }

    @Test
    @DisplayName("거래가 있는 경우 - 정상적으로 평균가 계산 및 저장")
    void aggregateHourlyAvgPrice_WithContracts_Success() throws JsonProcessingException {
        // given
        int contractCount = 5;
        String expectedJson = "{\"status\":\"SUCCESS\"}";

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenReturn(contractCount);
        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(contractHourlyAvgPriceRepository, never())
                .findLatestAvgPrice(any());
        verify(contractHourlyAvgPriceRepository, never())
                .insertHourlyAvgPriceWithValue(any(), any());

        // Redis 메시지 발행 확인
        verify(stringRedisTemplate, times(1))
                .convertAndSend(eq(CONTRACT_AGGREGATION_STATUS_CHANNEL), eq(expectedJson));
    }

    @Test
    @DisplayName("거래가 없고 이전 가격이 있는 경우 - 이전 가격 사용")
    void aggregateHourlyAvgPrice_NoContractsWithPreviousPrice_Success() throws JsonProcessingException {
        // given
        int contractCount = 0;
        Long previousPrice = 150000L;
        String expectedJson = "{\"status\":\"SUCCESS\"}";

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenReturn(contractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any()))
                .thenReturn(previousPrice);
        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(contractHourlyAvgPriceRepository, never())
                .insertHourlyAvgPrice(any(), any());
        verify(contractHourlyAvgPriceRepository, times(1))
                .findLatestAvgPrice(any(LocalDateTime.class));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPriceWithValue(any(LocalDateTime.class), eq(previousPrice));

        // Price 조회는 하지 않아야 함
        verify(priceRepository, never()).findById(any());

        // Redis 메시지 발행 확인
        verify(stringRedisTemplate, times(1))
                .convertAndSend(eq(CONTRACT_AGGREGATION_STATUS_CHANNEL), eq(expectedJson));
    }

    @Test
    @DisplayName("거래도 없고 이전 가격도 없는 경우 - 기본 가격 사용")
    void aggregateHourlyAvgPrice_NoContractsNoPreviousPrice_UseDefaultPrice() throws JsonProcessingException {
        // given
        int contractCount = 0;
        Long minimumPrice = 100000L;
        String expectedJson = "{\"status\":\"SUCCESS\"}";

        // Mock Price 객체 생성
        com.ureka.team3.utong_scheduler.price.entity.Price defaultPrice =
                mock(com.ureka.team3.utong_scheduler.price.entity.Price.class);
        when(defaultPrice.getMinimumPrice()).thenReturn(minimumPrice);

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenReturn(contractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any()))
                .thenReturn(null);
        when(priceRepository.findById(eq(PRICE_ID)))
                .thenReturn(Optional.of(defaultPrice));
        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(contractHourlyAvgPriceRepository, never())
                .insertHourlyAvgPrice(any(), any());
        verify(contractHourlyAvgPriceRepository, times(1))
                .findLatestAvgPrice(any(LocalDateTime.class));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPriceWithValue(any(LocalDateTime.class), eq(minimumPrice));

        // Price 조회 확인
        verify(priceRepository, times(1)).findById(PRICE_ID);

        // Redis 메시지 발행 확인
        verify(stringRedisTemplate, times(1))
                .convertAndSend(eq(CONTRACT_AGGREGATION_STATUS_CHANNEL), eq(expectedJson));
    }

    @Test
    @DisplayName("기본 가격 정보가 없는 경우 - 예외 발생")
    void aggregateHourlyAvgPrice_NoDefaultPrice_ThrowsException() throws JsonProcessingException {
        // given
        int contractCount = 0;
        String expectedJson = "{\"status\":\"FAILED\"}";

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenReturn(contractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any()))
                .thenReturn(null);
        when(priceRepository.findById(eq(PRICE_ID)))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(contractHourlyAvgPriceRepository, times(1))
                .findLatestAvgPrice(any(LocalDateTime.class));
        verify(priceRepository, times(1)).findById(PRICE_ID);

        // 실패 메시지 발행 확인
        verify(objectMapper, times(1)).writeValueAsString(argThat(message -> {
            AggregationStatusMessage msg = (AggregationStatusMessage) message;
            return "FAILED".equals(msg.getStatus()) &&
                    msg.getMessage().contains("계약 평균가 집계 실패");
        }));

        verify(stringRedisTemplate, times(1))
                .convertAndSend(eq(CONTRACT_AGGREGATION_STATUS_CHANNEL), eq(expectedJson));
    }

    @Test
    @DisplayName("Repository에서 예외 발생 시 - 실패 메시지 발행")
    void aggregateHourlyAvgPrice_RepositoryException_SendsFailedMessage() throws JsonProcessingException {
        // given
        String errorMessage = "Database connection failed";
        RuntimeException repositoryException = new RuntimeException(errorMessage);
        String expectedJson = "{\"status\":\"FAILED\"}";

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenThrow(repositoryException);
        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(objectMapper, times(1)).writeValueAsString(argThat(message -> {
            AggregationStatusMessage msg = (AggregationStatusMessage) message;
            return "FAILED".equals(msg.getStatus()) &&
                    msg.getMessage().contains(errorMessage);
        }));

        verify(stringRedisTemplate, times(1))
                .convertAndSend(eq(CONTRACT_AGGREGATION_STATUS_CHANNEL), eq(expectedJson));
    }

    @Test
    @DisplayName("Redis 메시지 발행 실패 시 - 예외 전파하지 않음")
    void aggregateHourlyAvgPrice_RedisPublishFailed_DoesNotThrow() throws JsonProcessingException {
        // given
        int contractCount = 3;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenReturn(contractCount);
        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenThrow(new JsonProcessingException("JSON serialization failed") {});

        // when & then (예외가 발생하지 않아야 함)
        assertDoesNotThrow(() -> contractAggregationScheduler.aggregateHourlyAvgPrice());

        // Repository 메서드는 정상 호출되어야 함
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("성공 메시지 내용 검증")
    void publishAggregationComplete_MessageContent() throws Exception {
        // given
        LocalDateTime testTime = LocalDateTime.of(2025, 7, 17, 18, 0, 0);
        String expectedJson = "{\"status\":\"SUCCESS\"}";

        when(objectMapper.writeValueAsString(any(AggregationStatusMessage.class)))
                .thenReturn(expectedJson);

        // when
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
                eq(CONTRACT_AGGREGATION_STATUS_CHANNEL),
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
                eq(CONTRACT_AGGREGATION_STATUS_CHANNEL),
                eq(expectedJson)
        );
    }

    @Test
    @DisplayName("시간 계산 로직 검증")
    void aggregateHourlyAvgPrice_TimeCalculation() {
        // given
        int contractCount = 1;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenReturn(contractCount);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(contractHourlyAvgPriceRepository).countContractsByTimeRange(
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
    @DisplayName("currentHour를 aggregatedAt으로 사용하는지 검증")
    void aggregateHourlyAvgPrice_UsesCurrentHourAsAggregatedAt() throws Exception {
        // given
        int contractCount = 0;
        Long previousPrice = 150000L;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any()))
                .thenReturn(contractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any()))
                .thenReturn(previousPrice);

        // when
        contractAggregationScheduler.aggregateHourlyAvgPrice();

        // then
        verify(contractHourlyAvgPriceRepository).insertHourlyAvgPriceWithValue(
                argThat(aggregatedAt ->
                        aggregatedAt.getMinute() == 0 &&
                                aggregatedAt.getSecond() == 0 &&
                                aggregatedAt.getNano() == 0
                ),
                eq(previousPrice)
        );

        // publishAggregationComplete도 currentHour로 호출되는지 확인
        verify(objectMapper).writeValueAsString(argThat(message -> {
            AggregationStatusMessage msg = (AggregationStatusMessage) message;
            return msg.getAggregatedAt().getMinute() == 0 &&
                    msg.getAggregatedAt().getSecond() == 0 &&
                    msg.getAggregatedAt().getNano() == 0;
        }));
    }
}

