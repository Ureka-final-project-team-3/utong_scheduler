package com.ureka.team3.utong_scheduler.trade.chart.service;

import com.ureka.team3.utong_scheduler.price.entity.Price;
import com.ureka.team3.utong_scheduler.price.repository.PriceRepository;
import com.ureka.team3.utong_scheduler.trade.chart.repository.ContractHourlyAvgPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AggregationServiceImplTest {

    @Mock
    private ContractHourlyAvgPriceRepository contractHourlyAvgPriceRepository;

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private AggregationServiceImpl aggregationService;

    private LocalDateTime currentHour;
    private LocalDateTime previousHour;
    private String dataCode;
    private static final String TEST_PRICE_ID = "903ee67c-71b3-432e-bbd1-aaf5d5043376";

    @BeforeEach
    void setUp() {
        currentHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        previousHour = currentHour.minusHours(1);
        dataCode = "001";
    }

    @Nested
    @DisplayName("시간별 집계 테스트")
    class AggregateHourly {

        @Test
        @DisplayName("성공 - 계약이 있는 경우")
        void aggregateHourly_성공_계약있음_test() {
            // given
            given(contractHourlyAvgPriceRepository.countContractsByTimeRange(previousHour, currentHour, dataCode))
                    .willReturn(5); // 계약이 5개 있다고 가정

            // when
            aggregationService.aggregateHourly(currentHour, previousHour, dataCode);

            // then
            then(contractHourlyAvgPriceRepository).should()
                    .insertHourlyAvgPrice(previousHour, currentHour, dataCode);
            then(contractHourlyAvgPriceRepository).should(never())
                    .insertHourlyAvgPriceWithValue(any(), any(), anyString());
        }

        @Test
        @DisplayName("성공 - 계약이 없고 이전 가격이 있는 경우")
        void aggregateHourly_성공_계약없음_이전가격있음_test() {
            // given
            given(contractHourlyAvgPriceRepository.countContractsByTimeRange(previousHour, currentHour, dataCode))
                    .willReturn(0); // 계약이 없음
            given(contractHourlyAvgPriceRepository.findLatestAvgPrice(previousHour, dataCode))
                    .willReturn(1500L); // 이전 가격이 있음

            // when
            aggregationService.aggregateHourly(currentHour, previousHour, dataCode);

            // then
            then(contractHourlyAvgPriceRepository).should(never())
                    .insertHourlyAvgPrice(any(), any(), anyString());
            then(contractHourlyAvgPriceRepository).should()
                    .insertHourlyAvgPriceWithValue(eq(currentHour), eq(1500L), eq(dataCode));
            then(priceRepository).should(never()).findById(anyString());
        }

        @Test
        @DisplayName("성공 - 계약이 없고 이전 가격도 없는 경우")
        void aggregateHourly_성공_계약없음_이전가격없음_test() {
            // given
            given(contractHourlyAvgPriceRepository.countContractsByTimeRange(previousHour, currentHour, dataCode))
                    .willReturn(0); // 계약이 없음
            given(contractHourlyAvgPriceRepository.findLatestAvgPrice(previousHour, dataCode))
                    .willReturn(null); // 이전 가격이 없음

            // Mock Price entity
            Price mockPrice = org.mockito.Mockito.mock(Price.class);
            given(mockPrice.getMinimumPrice()).willReturn(1000L);

            given(priceRepository.findById(TEST_PRICE_ID))
                    .willReturn(Optional.of(mockPrice));

            // when
            aggregationService.aggregateHourly(currentHour, previousHour, dataCode);

            // then
            then(contractHourlyAvgPriceRepository).should(never())
                    .insertHourlyAvgPrice(any(), any(), anyString());
            then(contractHourlyAvgPriceRepository).should()
                    .insertHourlyAvgPriceWithValue(eq(currentHour), eq(1000L), eq(dataCode));
            then(priceRepository).should().findById(TEST_PRICE_ID);
        }

        @Test
        @DisplayName("실패 - 기본 가격 정보가 없는 경우")
        void aggregateHourly_실패_기본가격없음_test() {
            // given
            given(contractHourlyAvgPriceRepository.countContractsByTimeRange(previousHour, currentHour, dataCode))
                    .willReturn(0); // 계약이 없음
            given(contractHourlyAvgPriceRepository.findLatestAvgPrice(previousHour, dataCode))
                    .willReturn(null); // 이전 가격이 없음
            given(priceRepository.findById(TEST_PRICE_ID))
                    .willReturn(Optional.empty()); // 기본 가격 정보가 없음

            // when & then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                aggregationService.aggregateHourly(currentHour, previousHour, dataCode);
            });

            assertThat(exception.getMessage()).isEqualTo("기본 가격 정보가 없습니다.");
        }
    }

    @Nested
    @DisplayName("데이터 개수 조회 테스트")
    class GetDataCountInRange {

        @Test
        @DisplayName("성공 - 데이터 개수 조회")
        void getDataCountInRange_성공_test() {
            // given
            LocalDateTime from = LocalDateTime.now().minusHours(8);
            LocalDateTime to = LocalDateTime.now();
            given(contractHourlyAvgPriceRepository.countContractHourlyAvgPriceByTimeRange(from, to, dataCode))
                    .willReturn(5);

            // when
            int count = aggregationService.getDataCountInRange(from, to, dataCode);

            // then
            assertThat(count).isEqualTo(5);
            then(contractHourlyAvgPriceRepository).should()
                    .countContractHourlyAvgPriceByTimeRange(from, to, dataCode);
        }

        @Test
        @DisplayName("성공 - 데이터가 없는 경우")
        void getDataCountInRange_성공_데이터없음_test() {
            // given
            LocalDateTime from = LocalDateTime.now().minusHours(8);
            LocalDateTime to = LocalDateTime.now();
            given(contractHourlyAvgPriceRepository.countContractHourlyAvgPriceByTimeRange(from, to, dataCode))
                    .willReturn(0);

            // when
            int count = aggregationService.getDataCountInRange(from, to, dataCode);

            // then
            assertThat(count).isEqualTo(0);
            then(contractHourlyAvgPriceRepository).should()
                    .countContractHourlyAvgPriceByTimeRange(from, to, dataCode);
        }
    }
}
