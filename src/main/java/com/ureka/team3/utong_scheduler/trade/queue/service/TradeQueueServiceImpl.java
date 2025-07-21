package com.ureka.team3.utong_scheduler.trade.queue.service;

import com.ureka.team3.utong_scheduler.common.entity.Code;
import com.ureka.team3.utong_scheduler.trade.global.config.DataTradePolicy;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrderDto;
import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;
import com.ureka.team3.utong_scheduler.trade.queue.repository.TradeQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;


@Service
@RequiredArgsConstructor
public class TradeQueueServiceImpl implements TradeQueueService {
    private final TradeQueueRepository tradeQueueRepository;
    private final DataTradePolicy dataTradePolicy;

    @Override
    public Map<Long, Long> getAllSellOrderNumbers(String dataCode) {
        Map<Long, List<OrderDto>> allSellOrders = tradeQueueRepository.findAllSellOrders(dataCode);
        Map<Long,Long> quantityByPrice = new TreeMap<>();
        for (Long price : allSellOrders.keySet()) {
            List<OrderDto> orderDtoList = allSellOrders.get(price);
            long sum = orderDtoList.stream()
                    .mapToLong(OrderDto::getQuantity)
                    .sum();

            quantityByPrice.put(price, sum);
        }

        return quantityByPrice;
    }

    @Override
    public Map<Long, Long> getAllBuyOrderNumbers(String dataCode) {
        Map<Long, List<OrderDto>> allBuyOrders = tradeQueueRepository.findAllBuyOrders(dataCode);
        Map<Long,Long> quantityByPrice = new TreeMap<>();
        for (Long price : allBuyOrders.keySet()) {
            List<OrderDto> orderDtoList = allBuyOrders.get(price);
            long sum = orderDtoList.stream()
                    .mapToLong(OrderDto::getQuantity)
                    .sum();

            quantityByPrice.put(price, sum);
        }

        return quantityByPrice;
    }

    @Override
    public void saveAllOrdersNumber(Map<String, OrdersQueueDto> dataMap) {
        for (Code code : dataTradePolicy.getDataTypeCodeList()) {
            String dataCode = code.getCode();
            OrdersQueueDto ordersQueueDto = dataMap.get(dataCode);
            tradeQueueRepository.saveAllBuyOrdersNumber(dataCode,ordersQueueDto.getBuyOrderQuantity());
            tradeQueueRepository.saveAllSellOrdersNumber(dataCode, ordersQueueDto.getSellOrderQuantity());

        }
    }
}
