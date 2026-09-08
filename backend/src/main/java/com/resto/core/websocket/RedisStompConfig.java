package com.resto.core.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Redis pub/sub fan-out so STOMP events reach every app instance.
 * Activate with {@code restoos.redis.broker-enabled=true}.
 * Redis Boot autoconfig stays excluded so single-node / test profiles do not require Redis.
 */
@Configuration
@ConditionalOnProperty(name = "restoos.redis.broker-enabled", havingValue = "true")
public class RedisStompConfig {

    public static final String CHANNEL = "restoos:stomp";

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisStompFanIn fanIn) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(fanIn, new ChannelTopic(CHANNEL));
        return container;
    }

    @Component
    @ConditionalOnProperty(name = "restoos.redis.broker-enabled", havingValue = "true")
    public static class RedisStompFanIn implements MessageListener {
        private static final Logger log = LoggerFactory.getLogger(RedisStompFanIn.class);
        private final SimpMessagingTemplate messagingTemplate;
        private final ObjectMapper objectMapper;

        public RedisStompFanIn(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
            this.messagingTemplate = messagingTemplate;
            this.objectMapper = objectMapper;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                Map<?, ?> envelope = objectMapper.readValue(body, Map.class);
                messagingTemplate.convertAndSend(
                        String.valueOf(envelope.get("destination")),
                        envelope.get("payload"));
            } catch (Exception e) {
                log.warn("Redis STOMP fan-in failed: {}", e.getMessage());
            }
        }
    }

    @Component
    @ConditionalOnProperty(name = "restoos.redis.broker-enabled", havingValue = "true")
    public static class RedisStompFanOut {
        private static final Logger log = LoggerFactory.getLogger(RedisStompFanOut.class);
        private final StringRedisTemplate redisTemplate;
        private final ObjectMapper objectMapper;

        public RedisStompFanOut(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
            this.redisTemplate = redisTemplate;
            this.objectMapper = objectMapper;
        }

        public void publish(String destination, Object payload) {
            try {
                redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(Map.of(
                        "destination", destination,
                        "payload", payload
                )));
            } catch (Exception e) {
                log.warn("Redis STOMP fan-out failed: {}", e.getMessage());
            }
        }
    }
}
