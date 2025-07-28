

package com.ureka.team3.utong_scheduler.trade.chart.scheduler;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.publisher.AggregationPublisher;
import com.ureka.team3.utong_scheduler.trade.chart.dto.AvgPerHour;
import com.ureka.team3.utong_scheduler.trade.chart.service.AggregationService;
import com.ureka.team3.utong_scheduler.trade.chart.service.CurrentPriceService;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ContractAggregationSchedulerTest {

    @Mock
    private AggregationPublisher aggregationPublisher;

    @Mock
    private DataTradePolicy dataTradePolicy;

    @Mock
    private AggregationService aggregationService;

    @Mock
    private CurrentPriceService currentPriceService;

    @InjectMocks
    private ContractAggregationScheduler contractAggregationScheduler;

    private List<Code> mockDataTypeCodeList;
    private Map<String, List<AvgPerHour>> mockDataMap;

    @BeforeEach
    void setUp() {
        // Mock data setup
        mockDataTypeCodeList = List.of(
                new Code("001", "001", "데이터 타입 1", "데이터 타입 1 설명", 1),
                new Code("002", "002", "데이터 타입 2", "데이터 타입 2 설명", 2)
        );

        mockDataMap = new HashMap<>();
        for (Code code : mockDataTypeCodeList) {
            List<AvgPerHour> avgPerHours = new ArrayList<>();
            avgPerHours.add(AvgPerHour.builder()
                    .dataCode(code.getCode())
                    .avgPrice(1000L)
                    .aggregatedAt(LocalDateTime.now())
                    .build());
            mockDataMap.put(code.getCode(), avgPerHours);
        }
    }

    @Nested
    @DisplayName("계약 평균가 집계 스케줄러")
    class HandleAggregation {

        @Test
        @DisplayName("성공 - 정상 집계 처리")
        void handleAggregation_성공_정상집계_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);

            for (Code code : mockDataTypeCodeList) {
                given(currentPriceService.getUpdatedData(code.getCode())).willReturn(mockDataMap.get(code.getCode()));
            }

            // when
            contractAggregationScheduler.handleAggregation();

            // then
            then(aggregationService).should(times(mockDataTypeCodeList.size()))
                    .aggregateHourly(any(LocalDateTime.class), any(LocalDateTime.class), anyString());
            then(currentPriceService).should(times(mockDataTypeCodeList.size()))
                    .updateRedisCache(anyString(), any(LocalDateTime.class));
            then(currentPriceService).should(times(mockDataTypeCodeList.size()))
                    .getUpdatedData(anyString());
            then(aggregationPublisher).should().publish(any(LocalDateTime.class), any(Map.class));
        }

        @Test
        @DisplayName("실패 - 집계 중 예외 발생")
        void handleAggregation_실패_예외발생_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);
            doThrow(new RuntimeException("집계 중 오류 발생")).when(aggregationService)
                    .aggregateHourly(any(LocalDateTime.class), any(LocalDateTime.class), anyString());

            // when
            contractAggregationScheduler.handleAggregation();

            // then
            then(aggregationPublisher).should().noticeFailed(anyString());
        }

        @Test
        @DisplayName("검증 - 시간 계산 정확성")
        void handleAggregation_검증_시간계산_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);

            // when
            LocalDateTime beforeExecution = LocalDateTime.now();
            contractAggregationScheduler.handleAggregation();
            LocalDateTime afterExecution = LocalDateTime.now();

            // then
            ArgumentCaptor<LocalDateTime> currentHourCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> previousHourCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            then(aggregationService).should().aggregateHourly(
                    currentHourCaptor.capture(), 
                    previousHourCaptor.capture(), 
                    eq(mockDataTypeCodeList.get(0).getCode())
            );

            LocalDateTime capturedCurrentHour = currentHourCaptor.getValue();
            LocalDateTime capturedPreviousHour = previousHourCaptor.getValue();

            // 현재 시간이 올바르게 계산되었는지 확인
            assertThat(capturedCurrentHour.getMinute()).isEqualTo(0);
            assertThat(capturedCurrentHour.getSecond()).isEqualTo(0);
            assertThat(capturedCurrentHour.getNano()).isEqualTo(0);

            // 이전 시간이 현재 시간보다 정확히 1시간 전인지 확인
            assertThat(capturedPreviousHour).isEqualTo(capturedCurrentHour.minusHours(1));
        }
    }

    @Nested
    @DisplayName("초기 데이터 삽입")
    class InsertInitialData {

        @Test
        @DisplayName("성공 - 초기 데이터 삽입")
        void insertInitialData_성공_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);
            given(aggregationService.getDataCountInRange(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                    .willReturn(0); // 데이터가 없는 경우

            // when
            contractAggregationScheduler.insertInitialData();

            // then
            // 각 데이터 코드에 대해 CHART_LIST_SIZE - 0 - 1 = 7번 호출되어야 함
            then(aggregationService).should(times(mockDataTypeCodeList.size() * DataTradePolicy.CHART_LIST_SIZE))
                    .aggregateHourly(any(LocalDateTime.class), any(LocalDateTime.class), anyString());
        }


        @Test
        @DisplayName("검증 - 시간 범위 계산")
        void insertInitialData_검증_시간범위_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(List.of(mockDataTypeCodeList.get(0)));
            given(aggregationService.getDataCountInRange(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                    .willReturn(0);

            // when
            contractAggregationScheduler.insertInitialData();

            // then
            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            then(aggregationService).should().getDataCountInRange(
                    fromCaptor.capture(), 
                    toCaptor.capture(), 
                    anyString()
            );

            LocalDateTime capturedFrom = fromCaptor.getValue();
            LocalDateTime capturedTo = toCaptor.getValue();

            // 시작 시간이 종료 시간보다 CHART_LIST_SIZE 시간 이전인지 확인
            assertThat(capturedFrom).isEqualTo(capturedTo.minusHours(DataTradePolicy.CHART_LIST_SIZE));
        }
    }
}
