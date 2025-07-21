package com.ureka.team3.utong_scheduler.trade.chart.service;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.trade.chart.dto.AvgPerHour;
import com.ureka.team3.utong_scheduler.trade.chart.repository.ContractHourlyAvgPriceRedisRepository;
import com.ureka.team3.utong_scheduler.trade.chart.repository.ContractHourlyAvgPriceRepository;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.global.entity.ContractHourlyAvgPrice;
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
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CurrentPriceServiceImplTest {

    @Mock
    private ContractHourlyAvgPriceRepository contractHourlyAvgPriceRepository;

    @Mock
    private ContractHourlyAvgPriceRedisRepository contractHourlyAvgPriceRedisRepository;

    @Mock
    private DataTradePolicy dataTradePolicy;

    @InjectMocks
    private CurrentPriceServiceImpl currentPriceService;

    private String dataCode;
    private LocalDateTime aggregatedAt;
    private List<Code> mockDataTypeCodeList;
    private List<ContractHourlyAvgPrice> mockContractHourlyAvgPrices;
    private List<AvgPerHour> mockAvgPerHours;

    @BeforeEach
    void setUp() {
        dataCode = "001";
        aggregatedAt = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        
        // Mock data setup
        mockDataTypeCodeList = List.of(
                new Code("001", "001", "데이터 타입 1", "데이터 타입 1 설명", 1),
                new Code("002", "002", "데이터 타입 2", "데이터 타입 2 설명", 2)
        );

        // Mock ContractHourlyAvgPrice list
        mockContractHourlyAvgPrices = new ArrayList<>();
        ContractHourlyAvgPrice contractHourlyAvgPrice = mock(ContractHourlyAvgPrice.class);
        mockContractHourlyAvgPrices.add(contractHourlyAvgPrice);
        
        // Mock AvgPerHour list
        mockAvgPerHours = new ArrayList<>();
        mockAvgPerHours.add(AvgPerHour.builder()
                .dataCode(dataCode)
                .avgPrice(1000L)
                .aggregatedAt(aggregatedAt)
                .build());
    }

    @Nested
    @DisplayName("Redis 캐시 업데이트 테스트")
    class UpdateRedisCache {

        @Test
        @DisplayName("성공 - Redis 캐시 업데이트")
        void updateRedisCache_성공_test() {
            // given
            given(contractHourlyAvgPriceRepository.findLatestByDataCodeBeforeTime(dataCode, aggregatedAt, 1))
                    .willReturn(mockContractHourlyAvgPrices);

            // when
            currentPriceService.updateRedisCache(dataCode, aggregatedAt);

            // then
            then(contractHourlyAvgPriceRepository).should()
                    .findLatestByDataCodeBeforeTime(dataCode, aggregatedAt, 1);
            then(contractHourlyAvgPriceRedisRepository).should()
                    .addDataWithSizeLimit(eq(dataCode), any(AvgPerHour.class), anyInt());
        }

    }

    @Nested
    @DisplayName("업데이트된 데이터 조회 테스트")
    class GetUpdatedData {

        @Test
        @DisplayName("성공 - 업데이트된 데이터 조회")
        void getUpdatedData_성공_test() {
            // given
            given(contractHourlyAvgPriceRedisRepository.getAllData(dataCode))
                    .willReturn(mockAvgPerHours);

            // when
            List<AvgPerHour> result = currentPriceService.getUpdatedData(dataCode);

            // then
            assertThat(result).isEqualTo(mockAvgPerHours);
            then(contractHourlyAvgPriceRedisRepository).should().getAllData(dataCode);
        }
    }

    @Nested
    @DisplayName("Redis 캐시 초기화 테스트")
    class Init {

        @Test
        @DisplayName("성공 - Redis 캐시 초기화")
        void init_성공_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);

            for (Code code : mockDataTypeCodeList) {
                given(contractHourlyAvgPriceRepository.findLatestByDataCodeBeforeTime(
                        eq(code.getCode()), any(LocalDateTime.class), anyInt()))
                        .willReturn(mockContractHourlyAvgPrices);
            }

            // when
            currentPriceService.init();

            // then
            then(dataTradePolicy).should().getDataTypeCodeList();
            then(contractHourlyAvgPriceRepository).should(times(mockDataTypeCodeList.size()))
                    .findLatestByDataCodeBeforeTime(anyString(), any(LocalDateTime.class), anyInt());
            then(contractHourlyAvgPriceRedisRepository).should(times(mockDataTypeCodeList.size()))
                    .initializeData(anyString(), any(List.class));
        }
    }
}