package com.studyroom.ranking;

import com.studyroom.ranking.message.UsageEventMessage;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * {@code usage-events} 전용 리스너 컨테이너 팩토리.
 *
 * <p>알림({@code notification-events})과 같은 브로커를 쓰지만 메시지 타입이 다르다.
 * 기본 컨슈머는 {@code NotificationMessage} 를 기본 타입으로 역직렬화하므로, 랭킹 워커는
 * {@code UsageEventMessage} 를 명시한 별도 팩토리를 쓴다.
 */
@Configuration
public class RankingKafkaConfig {

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, UsageEventMessage>
			usageEventKafkaListenerContainerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		JsonDeserializer<UsageEventMessage> valueDeserializer =
				new JsonDeserializer<>(UsageEventMessage.class);
		valueDeserializer.addTrustedPackages("com.studyroom.ranking.message");

		DefaultKafkaConsumerFactory<String, UsageEventMessage> consumerFactory =
				new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);

		ConcurrentKafkaListenerContainerFactory<String, UsageEventMessage> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		return factory;
	}
}
