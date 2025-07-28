package com.ureka.team3.utong_scheduler.trade.queue.repository;


import com.ureka.team3.utong_scheduler.trade.queue.dto.OrderDto;

import java.util.List;
import java.util.Map;

public interface TradeQueueRepository {
    Map<Long, List<OrderDto>> findAllSellOrders(String dataCode);

    Map<Long, List<OrderDto>> findAllBuyOrders(String dataCode);

    void saveAllSellOrdersNumber(String dataCode, Map<Long, Long> sellOrderQuantity);
    void saveAllBuyOrdersNumber(String dataCode, Map<Long, Long> buyOrderQuantity);

    void eraseAllSellOrdersNumber(String dataCode);
    void eraseAllBuyOrdersNumber(String dataCode);

    void changeCurrentSellOrderQuantity(String dataCode, Long price, Long amount);

    void changeCurrentBuyOrderQuantity(String dataCode, Long price, Long amount);

    Map<Long, Long> getAllBuyOrderQuantities(String dataCode);

    Map<Long, Long> getAllSellOrderQuantities(String dataCode);
}
