package com.gongsoop.global.config;

import com.gongsoop.studyspace.realtime.SeatEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis pub/sub 구성. 좌석 변화 이벤트는 애플리케이션 인스턴스가 늘어나도 모든 구독자에게
 * 전파되도록 Redis 채널을 거쳐 STOMP로 relay 한다. StringRedisTemplate 등 템플릿 빈은
 * Spring Boot 자동설정을 사용한다.
 */
@Configuration
public class RedisConfig {

    /** 좌석 변화 이벤트가 흐르는 Redis pub/sub 채널명. */
    public static final String SEAT_EVENTS_CHANNEL = "seat-events";

    @Bean
    public ChannelTopic seatEventsTopic() {
        return new ChannelTopic(SEAT_EVENTS_CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer seatEventListenerContainer(
            RedisConnectionFactory connectionFactory,
            SeatEventSubscriber seatEventSubscriber,
            ChannelTopic seatEventsTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(seatEventSubscriber, seatEventsTopic);
        return container;
    }
}
