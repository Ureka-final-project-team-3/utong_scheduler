package com.ureka.team3.utong_scheduler.config;

import com.ureka.team3.utong_scheduler.subscriber.TradeCanceledSubscriber;
import com.ureka.team3.utong_scheduler.subscriber.TradeExecutedSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private static final String Trade_CANCELED_CHANNEL = "trade:canceled";
//    private static final String TRADE_EXECUTED_CHANNEL = "trade:executed";

    private final TradeCanceledSubscriber tradeCanceledSubscriber;
    private final TradeExecutedSubscriber tradeExecutedSubscriber;
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                tradeCanceledSubscriber,
                new ChannelTopic(Trade_CANCELED_CHANNEL)
        );

        return container;
    }
}
