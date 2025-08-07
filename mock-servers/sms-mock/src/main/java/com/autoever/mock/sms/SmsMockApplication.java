package com.autoever.mock.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SMS API Mock 서버 애플리케이션
 * 포트 8082에서 실행되며 Basic Auth (autoever/5678)를 사용합니다.
 */
@SpringBootApplication
public class SmsMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsMockApplication.class, args);
    }
}