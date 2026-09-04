package com.studyroom.subscription;

import com.studyroom.subscription.message.SubscriptionEventMessage;
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
 * {@code subscription-events} 전용 리스너 컨테이너 팩토리 — 세 번째 이벤트 타입.
 * (6단계 {@code NotificationMessage}, 7단계 {@code UsageEventMessage} 와 같은 브로커, 다른 타입)
 */
@Configuration
public class SubscriptionKafkaConfig {

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, SubscriptionEventMessage>
			subscriptionEventKafkaListenerContainerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		JsonDeserializer<SubscriptionEventMessage> valueDeserializer =
				new JsonDeserializer<>(SubscriptionEventMessage.class);
		valueDeserializer.addTrustedPackages("com.studyroom.subscription.message");

		DefaultKafkaConsumerFactory<String, SubscriptionEventMessage> consumerFactory =
				new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);

		ConcurrentKafkaListenerContainerFactory<String, SubscriptionEventMessage> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		return factory;
	}
}
