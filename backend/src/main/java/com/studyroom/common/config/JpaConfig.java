package com.studyroom.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화. {@link com.studyroom.common.entity.BaseTimeEntity}의
 * createdAt/updatedAt 자동 기록에 필요하다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
