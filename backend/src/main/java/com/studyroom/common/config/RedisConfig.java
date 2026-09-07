package com.studyroom.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Value("${spring.data.redis.password:}")
	private String redisPassword;

	@Value("${spring.data.redis.ssl.enabled:false}")
	private boolean redisSsl;

	/**
	 * 일반 캐싱(홀딩 상태, 룸 상태 등)에 사용할 RedisTemplate.
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}

	/**
	 * 분산락(RLock)에 사용할 RedissonClient. 예약 동시성 제어(2단계)에서 사용.
	 */
	@Bean(destroyMethod = "shutdown")
	public RedissonClient redissonClient() {
		Config config = new Config();
		String scheme = redisSsl ? "rediss://" : "redis://";
		var server = config.useSingleServer()
				.setAddress(scheme + redisHost + ":" + redisPort);
		if (!redisPassword.isBlank()) {
			server.setPassword(redisPassword);
		}
		return Redisson.create(config);
	}
}
