package com.studyroom.support;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** WebSocket/STOMP 통합 테스트 헬퍼. */
public final class StompTestClient {

	private StompTestClient() {
	}

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StompTestClient.class);

	public static StompSession connect(int port) throws Exception {
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		// 서버 브로커 컨버터와 동일하게 java.time 모듈 등록 (Instant 역직렬화)
		converter.setObjectMapper(Jackson2ObjectMapperBuilder.json().build());
		client.setMessageConverter(converter);
		return client.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
			@Override
			public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command,
					StompHeaders headers, byte[] payload, Throwable exception) {
				log.error("[STOMP] handleException cmd={} payload={}", command,
						new String(payload, java.nio.charset.StandardCharsets.UTF_8), exception);
			}

			@Override
			public void handleTransportError(StompSession session, Throwable exception) {
				log.error("[STOMP] transport error", exception);
			}
		}).get(5, TimeUnit.SECONDS);
	}

	/** 구독 후 프레임을 큐로 흘려보낸다. 구독이 브로커에 반영될 시간을 잠깐 준다. */
	public static <T> BlockingQueue<T> subscribe(StompSession session, String destination, Class<T> type)
			throws InterruptedException {
		BlockingQueue<T> queue = new LinkedBlockingQueue<>();
		session.subscribe(destination, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return type;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void handleFrame(StompHeaders headers, Object payload) {
				queue.add((T) payload);
			}
		});
		Thread.sleep(200);
		return queue;
	}
}
