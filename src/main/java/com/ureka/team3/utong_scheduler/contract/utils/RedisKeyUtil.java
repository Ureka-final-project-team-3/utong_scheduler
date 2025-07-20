package com.ureka.team3.utong_scheduler.contract.utils;

public class RedisKeyUtil {

    public static final String ORDER_QUEUE_PREFIX = "order_queue";
    public static final String ORDER_BOOK_PREFIX = "order_book";
    public static final String CURRENT_PRICE_PREFIX = "current_price";

    public static String buildSellListKey(String dataCode, long price) {
        return ORDER_QUEUE_PREFIX + ":sell:" + dataCode + ":" + price;
    }

    public static String buildBuyListKey(String dataCode, long price) {
        return ORDER_QUEUE_PREFIX + ":buy:" + dataCode + ":" + price;
    }

    public static String buildCurrentPriceListKey(String dataCode) {
        return CURRENT_PRICE_PREFIX + ":" + dataCode;
    }

    public static String buildSellZSetKey(String dataCode) {
        return ORDER_BOOK_PREFIX + ":sell:" + dataCode;
    }

    public static String buildBuyZSetKey(String dataCode) {
        return ORDER_BOOK_PREFIX + ":buy:" + dataCode;
    }

    public static String buildOrderKey(String type, Long orderId) {
        return "order:" + type + ":" + orderId;
    }
}
