package com.ureka.team3.utong_scheduler.trade.queue.service;


import com.ureka.team3.utong_scheduler.trade.queue.dto.OrdersQueueDto;

import java.util.Map;

public interface TradeQueueService {

    // <가격, 수량>
    Map<Long, Long> getAllSellOrderNumbers(String dataCode);

    Map<Long, Long> getAllBuyOrderNumbers(String dataCode);

    void initAllOrdersNumber(Map<String, OrdersQueueDto> dataMap);
    void saveAllOrdersNumber(Map<String, OrdersQueueDto> dataMap);

    void changeCurrentDataAmount(String dataCode, long price, long sellChangeNumber, long purchaseChangeNumber);

    void init();
}
