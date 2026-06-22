package com.fredmaina.chatapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@SpringBootTest
class ChatappApplicationTests {

	@MockBean
	private RedisConnectionFactory redisConnectionFactory;

	@MockBean(name = "redisContainer")
	private RedisMessageListenerContainer redisContainer;

	@Test
	void contextLoads() {
	}

}
