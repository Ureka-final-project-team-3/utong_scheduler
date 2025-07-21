package com.ureka.team3.utong_scheduler.trade.queue.service;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrderDto;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
import com.ureka.team3.utong_scheduler.trade.queue.repository.TradeQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TradeQueueServiceImplTest {

    @Mock
    private TradeQueueRepository tradeQueueRepository;

    @Mock
    private DataTradePolicy dataTradePolicy;

    @InjectMocks
    private TradeQueueServiceImpl tradeQueueService;

    private String dataCode;
    private List<Code> mockDataTypeCodeList;
    private Map<Long, List<OrderDto>> mockBuyOrders;
    private Map<Long, List<OrderDto>> mockSellOrders;
    private Map<String, OrdersQueueDto> mockDataMap;

    @BeforeEach
    void setUp() {
        dataCode = "001";

        // Mock data setup
        mockDataTypeCodeList = List.of(
                new Code("001", "001", "데이터 타입 1", "데이터 타입 1 설명", 1),
                new Code("002", "002", "데이터 타입 2", "데이터 타입 2 설명", 2)
        );

        // Mock buy orders
        mockBuyOrders = new HashMap<>();
        List<OrderDto> buyOrders1 = new ArrayList<>();
        buyOrders1.add(OrderDto.builder().orderId("1").price(1000L).quantity(3L).dataCode(dataCode).build());
        buyOrders1.add(OrderDto.builder().orderId("2").price(1000L).quantity(2L).dataCode(dataCode).build());

        List<OrderDto> buyOrders2 = new ArrayList<>();
        buyOrders2.add(OrderDto.builder().orderId("3").price(1100L).quantity(4L).dataCode(dataCode).build());

        mockBuyOrders.put(1000L, buyOrders1);
        mockBuyOrders.put(1100L, buyOrders2);

        // Mock sell orders
        mockSellOrders = new HashMap<>();
        List<OrderDto> sellOrders1 = new ArrayList<>();
        sellOrders1.add(OrderDto.builder().orderId("4").price(1200L).quantity(2L).dataCode(dataCode).build());

        List<OrderDto> sellOrders2 = new ArrayList<>();
        sellOrders2.add(OrderDto.builder().orderId("5").price(1300L).quantity(3L).dataCode(dataCode).build());
        sellOrders2.add(OrderDto.builder().orderId("6").price(1300L).quantity(1L).dataCode(dataCode).build());

        mockSellOrders.put(1200L, sellOrders1);
        mockSellOrders.put(1300L, sellOrders2);

        // Mock data map
        mockDataMap = new HashMap<>();
        for (Code code : mockDataTypeCodeList) {
            Map<Long, Long> buyQuantity = new TreeMap<>();
            buyQuantity.put(1000L, 5L);
            buyQuantity.put(1100L, 4L);

            Map<Long, Long> sellQuantity = new TreeMap<>();
            sellQuantity.put(1200L, 2L);
            sellQuantity.put(1300L, 4L);

            OrdersQueueDto ordersQueueDto = OrdersQueueDto.builder()
                    .buyOrderQuantity(buyQuantity)
                    .sellOrderQuantity(sellQuantity)
                    .build();

            mockDataMap.put(code.getCode(), ordersQueueDto);
        }
    }

    @Nested
    @DisplayName("판매 주문 수량 조회 테스트")
    class GetAllSellOrderNumbers {

        @Test
        @DisplayName("성공 - 판매 주문 수량 조회")
        void getAllSellOrderNumbers_성공_test() {
            // given
            given(tradeQueueRepository.findAllSellOrders(dataCode)).willReturn(mockSellOrders);

            // when
            Map<Long, Long> result = tradeQueueService.getAllSellOrderNumbers(dataCode);

            // then
            then(tradeQueueRepository).should().findAllSellOrders(dataCode);

            // Verify the result contains the correct quantities
            assertThat(result).hasSize(2);
            assertThat(result.get(1200L)).isEqualTo(2L);
            assertThat(result.get(1300L)).isEqualTo(4L);
        }

        @Test
        @DisplayName("검증 - 수량 합산 확인")
        void getAllSellOrderNumbers_검증_수량합산_test() {
            // given
            given(tradeQueueRepository.findAllSellOrders(dataCode)).willReturn(mockSellOrders);

            // when
            Map<Long, Long> result = tradeQueueService.getAllSellOrderNumbers(dataCode);

            // then
            // 1300L 가격에 두 개의 주문(3L + 1L)이 있으므로 합계는 4L이어야 함
            assertThat(result.get(1300L)).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("구매 주문 수량 조회 테스트")
    class GetAllBuyOrderNumbers {

        @Test
        @DisplayName("성공 - 구매 주문 수량 조회")
        void getAllBuyOrderNumbers_성공_test() {
            // given
            given(tradeQueueRepository.findAllBuyOrders(dataCode)).willReturn(mockBuyOrders);

            // when
            Map<Long, Long> result = tradeQueueService.getAllBuyOrderNumbers(dataCode);

            // then
            then(tradeQueueRepository).should().findAllBuyOrders(dataCode);

            // Verify the result contains the correct quantities
            assertThat(result).hasSize(2);
            assertThat(result.get(1000L)).isEqualTo(5L);
            assertThat(result.get(1100L)).isEqualTo(4L);
        }

        @Test
        @DisplayName("검증 - 수량 합산 확인")
        void getAllBuyOrderNumbers_검증_수량합산_test() {
            // given
            given(tradeQueueRepository.findAllBuyOrders(dataCode)).willReturn(mockBuyOrders);

            // when
            Map<Long, Long> result = tradeQueueService.getAllBuyOrderNumbers(dataCode);

            // then
            // 1000L 가격에 두 개의 주문(3L + 2L)이 있으므로 합계는 5L이어야 함
            assertThat(result.get(1000L)).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("모든 주문 수량 저장 테스트")
    class SaveAllOrdersNumber {

        @Test
        @DisplayName("성공 - 모든 주문 수량 저장")
        void saveAllOrdersNumber_성공_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);

            // when
            tradeQueueService.saveAllOrdersNumber(mockDataMap);

            // then
            then(dataTradePolicy).should().getDataTypeCodeList();

            // Verify repository methods are called for each data code
            for (Code code : mockDataTypeCodeList) {
                OrdersQueueDto dto = mockDataMap.get(code.getCode());
                then(tradeQueueRepository).should().saveAllBuyOrdersNumber(
                        eq(code.getCode()), eq(dto.getBuyOrderQuantity()));
                then(tradeQueueRepository).should().saveAllSellOrdersNumber(
                        eq(code.getCode()), eq(dto.getSellOrderQuantity()));
            }
        }

        @Test
        @DisplayName("검증 - 모든 데이터 코드 처리")
        void saveAllOrdersNumber_검증_모든코드처리_test() {
            // given
            given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);

            // when
            tradeQueueService.saveAllOrdersNumber(mockDataMap);

            // then
            // 각 데이터 코드에 대해 buy와 sell 메서드가 한 번씩 호출되어야 함
            then(tradeQueueRepository).should(times(mockDataTypeCodeList.size()))
                    .saveAllBuyOrdersNumber(anyString(), anyMap());
            then(tradeQueueRepository).should(times(mockDataTypeCodeList.size()))
                    .saveAllSellOrdersNumber(anyString(), anyMap());
        }
    }
}
