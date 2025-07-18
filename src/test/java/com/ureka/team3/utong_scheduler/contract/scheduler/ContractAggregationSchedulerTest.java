package com.ureka.team3.utong_scheduler.contract.scheduler;

import com.ureka.team3.utong_scheduler.contract.repository.ContractHourlyAvgPriceRepository;
import com.ureka.team3.utong_scheduler.price.entity.Price;
import com.ureka.team3.utong_scheduler.price.repository.PriceRepository;
import com.ureka.team3.utong_scheduler.publisher.RedisPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractAggregationScheduler 테스트")
class ContractAggregationSchedulerTest {

    @Mock
    private ContractHourlyAvgPriceRepository contractHourlyAvgPriceRepository;

    @Mock
    private PriceRepository priceRepository;

    @Mock
    private RedisPublisher redisPublisher;

    @InjectMocks
    private ContractAggregationScheduler contractAggregationScheduler;

    private final String PRICE_ID = "903ee67c-71b3-432e-bbd1-aaf5d5043376";

    @BeforeEach
    void setUp() {
        // static final 필드는 리플렉션으로 변경할 수 없으므로 setUp에서 제거
        // 테스트에서 실제 상수 값을 직접 사용
    }

    @Test
    @DisplayName("LTE와 5G 모두 거래가 있는 경우 - 정상적으로 집계")
    void handleAggregation_BothDataTypesWithContracts_Success() {
        // given
        int lteContractCount = 5;
        int fiveGContractCount = 3;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), eq("001")))
                .thenReturn(lteContractCount);
        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), eq("002")))
                .thenReturn(fiveGContractCount);

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // LTE 집계 확인
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class), eq("001"));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class), eq("001"));

        // 5G 집계 확인
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class), eq("002"));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class), eq("002"));

        // Redis 메시지는 1번만 발행
        verify(redisPublisher, times(1))
                .publishAggregationComplete(any(LocalDateTime.class));
        verify(redisPublisher, never())
                .publishAggregationFailed(any());
    }

    @Test
    @DisplayName("LTE는 거래 있고 5G는 거래 없는 경우 - 혼합 처리")
    void handleAggregation_LteWithContracts5GWithoutContracts_MixedHandling() {
        // given
        int lteContractCount = 5;
        int fiveGContractCount = 0;
        Long previousPrice = 150000L;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), eq("001")))
                .thenReturn(lteContractCount);
        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), eq("002")))
                .thenReturn(fiveGContractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any(), eq("002")))
                .thenReturn(previousPrice);

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // LTE는 실제 집계
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(LocalDateTime.class), any(LocalDateTime.class), eq("001"));

        // 5G는 이전 가격 사용
        verify(contractHourlyAvgPriceRepository, times(1))
                .findLatestAvgPrice(any(LocalDateTime.class), eq("002"));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPriceWithValue(any(LocalDateTime.class), eq(previousPrice), eq("002"));

        // Price 조회는 하지 않아야 함
        verify(priceRepository, never()).findById(any());

        // Redis 메시지는 1번만 발행 (성공)
        verify(redisPublisher, times(1))
                .publishAggregationComplete(any(LocalDateTime.class));
        verify(redisPublisher, never())
                .publishAggregationFailed(any());
    }

    @Test
    @DisplayName("LTE와 5G 모두 거래가 없고 이전 가격이 없는 경우 - 기본 가격 사용")
    void handleAggregation_BothDataTypesNoPreviousPrice_UseDefaultPrice() {
        // given
        int contractCount = 0;
        Long minimumPrice = 100000L;

        Price defaultPrice = mock(Price.class);
        when(defaultPrice.getMinimumPrice()).thenReturn(minimumPrice);

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), anyString()))
                .thenReturn(contractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any(), anyString()))
                .thenReturn(null);
        when(priceRepository.findById(PRICE_ID))
                .thenReturn(Optional.of(defaultPrice));

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // 두 데이터 타입 모두 기본 가격 사용
        verify(contractHourlyAvgPriceRepository, times(2))
                .insertHourlyAvgPriceWithValue(any(LocalDateTime.class), eq(minimumPrice), anyString());

        // Price 조회는 2번 (LTE, 5G 각각)
        verify(priceRepository, times(2)).findById(PRICE_ID);

        // Redis 메시지는 1번만 발행 (성공)
        verify(redisPublisher, times(1))
                .publishAggregationComplete(any(LocalDateTime.class));
        verify(redisPublisher, never())
                .publishAggregationFailed(any());
    }

    @Test
    @DisplayName("기본 가격 정보가 없는 경우 - 예외 발생 및 실패 메시지 발행")
    void handleAggregation_NoDefaultPrice_ThrowsExceptionAndSendsFailedMessage() {
        // given
        int contractCount = 0;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), anyString()))
                .thenReturn(contractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any(), anyString()))
                .thenReturn(null);
        when(priceRepository.findById(PRICE_ID))
                .thenReturn(Optional.empty());

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // 실패 메시지 1번 발행
        verify(redisPublisher, times(1))
                .publishAggregationFailed(contains("기본 가격 정보가 없습니다"));

        // 성공 메시지는 발행되지 않아야 함
        verify(redisPublisher, never())
                .publishAggregationComplete(any());
    }

    @Test
    @DisplayName("Repository에서 예외 발생 시 - 실패 메시지 발행")
    void handleAggregation_RepositoryException_SendsFailedMessage() {
        // given
        String errorMessage = "Database connection failed";
        RuntimeException repositoryException = new RuntimeException(errorMessage);

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), anyString()))
                .thenThrow(repositoryException);

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        verify(redisPublisher, times(1))
                .publishAggregationFailed(errorMessage);

        // 성공 메시지는 발행되지 않아야 함
        verify(redisPublisher, never())
                .publishAggregationComplete(any());
    }

    @Test
    @DisplayName("시간 계산 로직 검증 - 정각으로 계산되는지 확인")
    void handleAggregation_TimeCalculation_UsesCorrectHours() {
        // given
        int contractCount = 1;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), anyString()))
                .thenReturn(contractCount);

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // LTE와 5G 각각에 대해 시간 계산 확인
        verify(contractHourlyAvgPriceRepository, times(2)).countContractsByTimeRange(
                argThat(previousHour ->
                        previousHour.getMinute() == 0 &&
                                previousHour.getSecond() == 0 &&
                                previousHour.getNano() == 0
                ),
                argThat(currentHour ->
                        currentHour.getMinute() == 0 &&
                                currentHour.getSecond() == 0 &&
                                currentHour.getNano() == 0
                ),
                anyString()
        );

        verify(contractHourlyAvgPriceRepository, times(2)).insertHourlyAvgPrice(
                argThat(previousHour ->
                        previousHour.getMinute() == 0 &&
                                previousHour.getSecond() == 0 &&
                                previousHour.getNano() == 0
                ),
                argThat(currentHour ->
                        currentHour.getMinute() == 0 &&
                                currentHour.getSecond() == 0 &&
                                currentHour.getNano() == 0
                ),
                anyString()
        );
    }

    @Test
    @DisplayName("데이터 코드별 처리 확인")
    void handleAggregation_DataCodeHandling_ProcessesBothLteAnd5G() {
        // given
        int contractCount = 3;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), anyString()))
                .thenReturn(contractCount);

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // LTE(001) 처리 확인
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(), any(), eq("001"));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(), any(), eq("001"));

        // 5G(002) 처리 확인
        verify(contractHourlyAvgPriceRepository, times(1))
                .countContractsByTimeRange(any(), any(), eq("002"));
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(), any(), eq("002"));

        // 성공 메시지 1번만 발행
        verify(redisPublisher, times(1))
                .publishAggregationComplete(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("currentHour를 aggregatedAt으로 사용하는지 검증")
    void handleAggregation_UsesCurrentHourAsAggregatedAt() {
        // given
        int contractCount = 0;
        Long previousPrice = 150000L;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), anyString()))
                .thenReturn(contractCount);
        when(contractHourlyAvgPriceRepository.findLatestAvgPrice(any(), anyString()))
                .thenReturn(previousPrice);

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // insertHourlyAvgPriceWithValue에서 currentHour가 aggregatedAt으로 사용되는지 확인
        verify(contractHourlyAvgPriceRepository, times(2)).insertHourlyAvgPriceWithValue(
                argThat(aggregatedAt ->
                        aggregatedAt.getMinute() == 0 &&
                                aggregatedAt.getSecond() == 0 &&
                                aggregatedAt.getNano() == 0
                ),
                eq(previousPrice),
                anyString()
        );

        // publishAggregationComplete도 currentHour로 호출되는지 확인
        verify(redisPublisher).publishAggregationComplete(
                argThat(aggregatedAt ->
                        aggregatedAt.getMinute() == 0 &&
                                aggregatedAt.getSecond() == 0 &&
                                aggregatedAt.getNano() == 0
                )
        );
    }

    @Test
    @DisplayName("RedisPublisher 실패 시에도 전체 로직은 완료")
    void handleAggregation_RedisPublisherFails_DoesNotAffectCoreLogic() {
        // given
        int contractCount = 2;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), anyString()))
                .thenReturn(contractCount);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(redisPublisher).publishAggregationComplete(any());

        // when & then (예외가 발생하지 않아야 함)
        assertDoesNotThrow(() -> contractAggregationScheduler.handleAggregation());

        // 핵심 비즈니스 로직은 정상 실행되어야 함
        verify(contractHourlyAvgPriceRepository, times(2))
                .insertHourlyAvgPrice(any(), any(), anyString());
    }

    @Test
    @DisplayName("LTE만 처리되고 5G에서 예외 발생하는 경우")
    void handleAggregation_PartialFailure_LteSucceeds5GFails() {
        // given
        int lteContractCount = 3;

        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), eq("001")))
                .thenReturn(lteContractCount);
        when(contractHourlyAvgPriceRepository.countContractsByTimeRange(any(), any(), eq("002")))
                .thenThrow(new RuntimeException("5G data access failed"));

        // when
        contractAggregationScheduler.handleAggregation();

        // then
        // LTE는 정상 처리되어야 함
        verify(contractHourlyAvgPriceRepository, times(1))
                .insertHourlyAvgPrice(any(), any(), eq("001"));

        // 전체 실패로 처리되어야 함
        verify(redisPublisher, times(1))
                .publishAggregationFailed(contains("5G data access failed"));
        verify(redisPublisher, never())
                .publishAggregationComplete(any());
    }
}