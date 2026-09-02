package com.studyroom.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket. 클라이언트는 {@code /ws}로 접속해 {@code /topic/rooms/{roomId}}를
 * 구독하고, 서버는 룸 현황이 바뀔 때 그 목적지로 {@code RoomChangeEvent}를 발행한다
 * (로드맵 4단계). 랭킹·추첨 등도 {@code /topic/*}로 확장 예정.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final String[] allowedOrigins;

	public WebSocketConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 네이티브 WebSocket (SockJS 폴백 없음) — REST와 동일한 오리진만 허용.
		registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
	}
}
