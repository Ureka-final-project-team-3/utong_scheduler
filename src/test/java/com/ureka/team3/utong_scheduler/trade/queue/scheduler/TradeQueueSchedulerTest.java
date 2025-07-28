//package com.ureka.team3.utong_scheduler.trade.queue.scheduler;
//
//import com.ureka.team3.utong_scheduler.common.entity.Code;
//import com.ureka.team3.utong_scheduler.publisher.TradeQueuePublisher;
//import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
//import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
//import com.ureka.team3.utong_scheduler.trade.queue.service.TradeQueueService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyMap;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.times;
//
//@ExtendWith(MockitoExtension.class)
//class TradeQueueSchedulerTest {
//
//    @Mock
//    private TradeQueuePublisher tradeQueuePublisher;
//
//    @Mock
//    private DataTradePolicy dataTradePolicy;
//
//    @Mock
//    private TradeQueueService tradeQueueService;
//
//    @InjectMocks
//    private TradeQueueScheduler tradeQueueScheduler;
//
//    private List<Code> mockDataTypeCodeList;
//    private Map<Long, Long> mockBuyOrderNumbers;
//    private Map<Long, Long> mockSellOrderNumbers;
//
//    @BeforeEach
//    void setUp() {
//        // Mock data setup
//        mockDataTypeCodeList = List.of(
//                new Code("001", "001", "데이터 타입 1", "데이터 타입 1 설명", 1),
//                new Code("002", "002", "데이터 타입 2", "데이터 타입 2 설명", 2)
//        );
//
//        // Mock buy order numbers
//        mockBuyOrderNumbers = new TreeMap<>();
//        mockBuyOrderNumbers.put(1000L, 5L);
//        mockBuyOrderNumbers.put(1100L, 3L);
//
//        // Mock sell order numbers
//        mockSellOrderNumbers = new TreeMap<>();
//        mockSellOrderNumbers.put(1200L, 2L);
//        mockSellOrderNumbers.put(1300L, 4L);
//    }
//
//    @Test
//    @DisplayName("성공 - 대기열 처리")
//    void handleAggregation_성공_test() {
//        // given
//        given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);
//
//        for (Code code : mockDataTypeCodeList) {
//            given(tradeQueueService.getAllBuyOrderNumbers(code.getCode())).willReturn(mockBuyOrderNumbers);
//            given(tradeQueueService.getAllSellOrderNumbers(code.getCode())).willReturn(mockSellOrderNumbers);
//        }
//
//        // when
//        tradeQueueScheduler.handleAggregation();
//
//        // then
//        then(dataTradePolicy).should().getDataTypeCodeList();
//        then(tradeQueueService).should(times(mockDataTypeCodeList.size())).getAllBuyOrderNumbers(anyString());
//        then(tradeQueueService).should(times(mockDataTypeCodeList.size())).getAllSellOrderNumbers(anyString());
//        then(tradeQueueService).should().saveAllOrdersNumber(anyMap());
//        then(tradeQueuePublisher).should().publish(any(LocalDateTime.class), anyMap());
//    }
//
//    @Test
//    @DisplayName("검증 - 데이터 맵 구성")
//    void handleAggregation_검증_데이터맵_test() {
//        // given
//        given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);
//
//        for (Code code : mockDataTypeCodeList) {
//            given(tradeQueueService.getAllBuyOrderNumbers(code.getCode())).willReturn(mockBuyOrderNumbers);
//            given(tradeQueueService.getAllSellOrderNumbers(code.getCode())).willReturn(mockSellOrderNumbers);
//        }
//
//        // when
//        tradeQueueScheduler.handleAggregation();
//
//        // then
//        ArgumentCaptor<Map<String, OrdersQueueDto>> dataMapCaptor = ArgumentCaptor.forClass(Map.class);
//        then(tradeQueueService).should().saveAllOrdersNumber(dataMapCaptor.capture());
//
//        Map<String, OrdersQueueDto> capturedDataMap = dataMapCaptor.getValue();
//
//        // Verify the data map contains entries for all data codes
//        assertThat(capturedDataMap).hasSize(mockDataTypeCodeList.size());
//
//        // Verify each entry has the correct buy and sell order numbers
//        for (Code code : mockDataTypeCodeList) {
//            OrdersQueueDto ordersQueueDto = capturedDataMap.get(code.getCode());
//            assertThat(ordersQueueDto).isNotNull();
//            assertThat(ordersQueueDto.getBuyOrderQuantity()).isEqualTo(mockBuyOrderNumbers);
//            assertThat(ordersQueueDto.getSellOrderQuantity()).isEqualTo(mockSellOrderNumbers);
//        }
//    }
//
//    @Test
//    @DisplayName("실패 - 예외 발생")
//    void handleAggregation_실패_예외발생_test() {
//        // given
//        given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);
//        doThrow(new RuntimeException("대기열 처리 중 오류 발생")).when(tradeQueueService).getAllBuyOrderNumbers(anyString());
//
//        // when
//        tradeQueueScheduler.handleAggregation();
//
//        // then
//        then(tradeQueuePublisher).should().noticeFailed(anyString());
//    }
//
//    @Test
//    @DisplayName("검증 - 시간 정확성")
//    void handleAggregation_검증_시간정확성_test() {
//        // given
//        given(dataTradePolicy.getDataTypeCodeList()).willReturn(mockDataTypeCodeList);
//
//        for (Code code : mockDataTypeCodeList) {
//            given(tradeQueueService.getAllBuyOrderNumbers(code.getCode())).willReturn(mockBuyOrderNumbers);
//            given(tradeQueueService.getAllSellOrderNumbers(code.getCode())).willReturn(mockSellOrderNumbers);
//        }
//
//        // when
//        LocalDateTime beforeExecution = LocalDateTime.now();
//        tradeQueueScheduler.handleAggregation();
//        LocalDateTime afterExecution = LocalDateTime.now();
//
//        // then
//        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
//        then(tradeQueuePublisher).should().publish(timeCaptor.capture(), anyMap());
//
//        LocalDateTime capturedTime = timeCaptor.getValue();
//        assertThat(capturedTime).isBetween(beforeExecution, afterExecution);
//    }
//}