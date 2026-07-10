package com.gongsoop;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(StudySpaceRealtimeProperties.class)
public class StudyforestApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyforestApplication.class, args);
    }
}
