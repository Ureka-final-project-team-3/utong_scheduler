package com.ureka.team3.utong_scheduler.publisher;

import java.time.LocalDateTime;

public interface RedisPublisher<T> {
    void publish(LocalDateTime localDateTime, T data);
    void noticeFailed(String errorMessage);
}
