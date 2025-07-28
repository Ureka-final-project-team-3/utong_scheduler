package com.ureka.team3.utong_scheduler.trade.queue.service;


import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;

import java.util.Map;

public interface TradeQueueService {

    // <가격, 수량>
    Map<Long, Long> getAllSellOrderNumbers(String dataCode);

    Map<Long, Long> getAllBuyOrderNumbers(String dataCode);

    Map<Long, Long> getAllBuyOrderCachedNumbers(String dataCode);

    Map<Long, Long> getAllSellOrderCachedNumbers(String dataCode);

    void initAllOrdersNumber(Map<String, OrdersQueueDto> dataMap);
    void saveAllOrdersNumber(Map<String, OrdersQueueDto> dataMap);

    void changeCurrentDataAmount(String dataCode, Map<Long, Long> saleDataChanges, Map<Long,Long> purchaseDataChanges);

    void init();
}
