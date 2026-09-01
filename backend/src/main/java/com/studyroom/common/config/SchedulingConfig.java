package com.studyroom.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @Scheduled} 활성화 — 홀딩 백스톱 스윕 등에 사용. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
