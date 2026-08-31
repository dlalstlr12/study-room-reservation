package com.studyroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudyRoomApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudyRoomApplication.class, args);
	}
}
