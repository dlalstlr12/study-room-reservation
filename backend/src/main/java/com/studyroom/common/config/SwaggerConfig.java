package com.studyroom.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI studyRoomOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("스터디룸 예약 시스템 API")
						.description("예약 / 이벤트 추첨 / 알림 / 실시간 랭킹 / 정기 구독권을 포함한 백엔드 포트폴리오 프로젝트")
						.version("v0.0.1"));
	}
}
