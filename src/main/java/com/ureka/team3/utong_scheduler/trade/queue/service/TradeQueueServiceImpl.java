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

            if(sum > 0L){
                quantityByPrice.put(price, sum);
            }

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

            if(sum>0L){
                quantityByPrice.put(price, sum);
            }

        }

        return quantityByPrice;
    }

    @Override
    public Map<Long, Long> getAllBuyOrderCachedNumbers(String dataCode) {
        return tradeQueueRepository.getAllBuyOrderQuantities(dataCode);
    }

    @Override
    public Map<Long, Long> getAllSellOrderCachedNumbers(String dataCode) {
        return tradeQueueRepository.getAllSellOrderQuantities(dataCode);
    }

    @Override
    public void initAllOrdersNumber(Map<String, OrdersQueueDto> dataMap) {
        for (Code code : dataTradePolicy.getDataTypeCodeList()) {
            String dataCode = code.getCode();
            OrdersQueueDto ordersQueueDto = dataMap.get(dataCode);

            tradeQueueRepository.eraseAllSellOrdersNumber(dataCode);
            tradeQueueRepository.eraseAllBuyOrdersNumber(dataCode);

            tradeQueueRepository.saveAllBuyOrdersNumber(dataCode,ordersQueueDto.getBuyOrderQuantity());
            tradeQueueRepository.saveAllSellOrdersNumber(dataCode, ordersQueueDto.getSellOrderQuantity());
        }
    }

    @Override
    public void saveAllOrdersNumber(Map<String, OrdersQueueDto> dataMap) {

    }

    @Override
    public void changeCurrentDataAmount(String dataCode, Map<Long, Long> saleDataChanges, Map<Long, Long> purchaseDataChanges) {
        for (Map.Entry<Long, Long> entry : saleDataChanges.entrySet()) {
            Long price = entry.getKey();
            Long amount = entry.getValue();
            tradeQueueRepository.changeCurrentSellOrderQuantity(dataCode,price,amount);
        }

        for (Map.Entry<Long, Long> entry : purchaseDataChanges.entrySet()) {
            Long price = entry.getKey();
            Long amount = entry.getValue();
            tradeQueueRepository.changeCurrentBuyOrderQuantity(dataCode,price,amount);
        }
    }

    @Override
    public void init() {
        for (Code code : dataTradePolicy.getDataTypeCodeList()) {
            Map<Long, Long> allBuyOrderNumbers = getAllBuyOrderNumbers(code.getCode());
            Map<Long, Long> allSellOrderNumbers = getAllSellOrderNumbers(code.getCode());

            tradeQueueRepository.saveAllBuyOrdersNumber(code.getCode(),allBuyOrderNumbers);
            tradeQueueRepository.saveAllSellOrdersNumber(code.getCode(), allSellOrderNumbers);
        }
    }
}
