package com.resto.unit;

import com.resto.core.websocket.RedisStompConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisStompConfigTest {

    @Test
    @DisplayName("Lettuce factory uses password-protected Redis DB index")
    void factoryUsesPasswordAndDatabase() {
        RedisStompConfig config = new RedisStompConfig();
        LettuceConnectionFactory factory = (LettuceConnectionFactory) config.redisConnectionFactory(
                "redis", 6379, "hub-secret", 9);
        try {
            factory.afterPropertiesSet();
            assertEquals("redis", factory.getHostName());
            assertEquals(6379, factory.getPort());
            assertEquals(9, factory.getDatabase());
            assertNotNull(factory);
        } finally {
            factory.destroy();
        }
    }
}
