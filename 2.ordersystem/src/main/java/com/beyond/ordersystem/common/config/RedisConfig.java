package com.beyond.ordersystem.common.config;

import com.beyond.ordersystem.common.service.SseAlarmService;
import com.fasterxml.jackson.databind.JsonSerializable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;

    @Bean
    // 같은 Bean 객체가 있을 경우 Bean 객체를 구분하기 위한 어노테이션
    @Qualifier("rtInventory")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(0);

        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("stockInventory")
    public RedisConnectionFactory stockConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(1);

        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("rtInventory")
    // Bean들끼리 서로 의존성을 주입받을 때, 메서드 파라미터로도 주입 가능
    // 모든 Template 중에 무조건 redisTemplate 이라는 메서드 명이 반드시 1개는 있어야 함.
    public RedisTemplate<String, String> redisTemplate(@Qualifier("rtInventory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory); // Factory 객체 연결
        return redisTemplate;
    }

    //    redis pub/sub을 위한 연결객체 생성
    @Bean
    @Qualifier("ssePubSub")
    public RedisConnectionFactory sseFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(2);  // SSE용은 database 2 사용
//      redis pub/sub기능은 Db에 값을 저장하는 기능이 아니므로, 특정 db에 의존적이지 않음.
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("stockInventory")
    public RedisTemplate<String, String> stockRedisTemplate(@Qualifier("stockInventory") RedisConnectionFactory stockConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(stockConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("ssePubSub")
    public RedisTemplate<String, String> sseRedisTemplate(@Qualifier("ssePubSub") RedisConnectionFactory sseConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(sseConnectionFactory);
        return redisTemplate;
    }

//    redis 리스너 객체
    @Bean
    @Qualifier("ssePubSub")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory,
                    MessageListenerAdapter messageListenerAdapter
    ){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("order-channel"));
//        만약에 여러 채널을 구독해야되는 경우, 여러개의 PattennTopic을 add하거나, 별도의 Bean객체 생성.
        return container;
    }

//    redis의 채널에서 수신된 메세지를 처리하는 빈 객체
    @Bean
    public MessageListenerAdapter messageListenerAdapter(SseAlarmService sseAlarmService){
//        채널로부터 수신되는 message처리를 SseAlarmService의 onMessage메서드로 설정
//        즉, 메세지가 수신되면 onMessage메서드가 호출
        return new MessageListenerAdapter(sseAlarmService,"onMessage");
    }

}
