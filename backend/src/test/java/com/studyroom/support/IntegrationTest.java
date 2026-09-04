package com.studyroom.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 베이스. 실제 MySQL·Redis·Kafka 컨테이너를 한 번 띄워 모든 하위 테스트가 공유한다
 * (싱글턴 컨테이너 패턴 — JUnit {@code @Container} 대신 static 시작, JVM 종료 시 Ryuk 정리).
 * 로컬에 {@code docker compose up} 이 없어도 동작한다.
 *
 * <p>Kafka는 알림(6단계) 테스트만 쓰지만, {@code @KafkaListener} 컨슈머가 컨텍스트 기동 시
 * 항상 브로커에 붙어야 하므로 베이스에 둔다.
 */
@SpringBootTest
@Testcontainers
public abstract class IntegrationTest {

	static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
			.withDatabaseName("study_room")
			.withReuse(true);

	static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
			.withExposedPorts(6379)
			.withReuse(true);

	static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"))
			.withReuse(true);

	static {
		MYSQL.start();
		REDIS.start();
		KAFKA.start();
	}

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		registry.add("spring.kafka.bootstrap-servers",
				() -> KAFKA.getBootstrapServers().replace("PLAINTEXT://", ""));
		// 아웃박스 릴레이 스케줄러는 테스트에서 끈다 — 테스트가 relay()를 직접 호출해 타이밍을 통제한다.
		registry.add("subscription.outbox.scheduler-enabled", () -> "false");
	}
}
