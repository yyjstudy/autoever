package com.autoever.mock.kakaotalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 카카오톡 API Mock 서버 애플리케이션
 * 포트 8081에서 실행되며 Basic Auth (autoever/1234)를 사용합니다.
 */
@SpringBootApplication
public class KakaoTalkMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(KakaoTalkMockApplication.class, args);
    }
}