package com.kanban.backend;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KanbanBackendApplication {

    public static void main(String[] args) {
        // 배포 서버의 OS/컨테이너 기본 타임존이 UTC일 수 있어, LocalDateTime.now()가
        // 의도와 다르게 UTC 기준으로 찍히는 것을 막기 위해 JVM 기본 타임존을 고정한다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(KanbanBackendApplication.class, args);
    }
}
