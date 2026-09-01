package com.studyroom.common.config;

import com.studyroom.reservation.hold.HoldExpirationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis keyspace 만료 알림을 켜고({@code notify-keyspace-events Ex}) 홀딩 만료 리스너를 등록한다.
 *
 * <p>설정을 앱 시작 시 프로그램으로 켜므로 docker-compose·Testcontainers의 Redis 커맨드를
 * 건드릴 필요가 없다(이식성).
 */
@Configuration
public class RedisKeyspaceConfig {

	private static final Logger log = LoggerFactory.getLogger(RedisKeyspaceConfig.class);

	@Bean
	public RedisMessageListenerContainer holdKeyspaceListenerContainer(
			RedisConnectionFactory connectionFactory, HoldExpirationListener holdExpirationListener) {

		try (RedisConnection connection = connectionFactory.getConnection()) {
			connection.serverCommands().setConfig("notify-keyspace-events", "Ex");
			log.info("Redis keyspace 만료 알림 활성화 (notify-keyspace-events=Ex)");
		} catch (RuntimeException e) {
			log.warn("notify-keyspace-events 설정 실패 — 홀딩 만료는 백스톱 스윕에만 의존합니다", e);
		}

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(holdExpirationListener, new PatternTopic("__keyevent@*__:expired"));
		return container;
	}
}
