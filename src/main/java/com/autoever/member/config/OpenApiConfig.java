package com.autoever.member.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 (Swagger) 설정 클래스
 * API 문서화 및 Basic Authentication 스키마 구성
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 설정 Bean
     * 
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("개발 환경 서버")
            ))
            .components(new Components()
                .addSecuritySchemes("basicAuth", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Basic Authentication (admin/1212)")
                )
            );
    }

    /**
     * API 정보 설정
     * 
     * @return API 메타 정보
     */
    private Info apiInfo() {
        return new Info()
            .title("AutoEver Member Management API")
            .description("""
                ## AutoEver 회원 관리 시스템 API
                
                ### 인증 방식
                - **Basic Authentication**: admin/1212 계정으로 관리자 기능 접근
                - 일반 회원가입은 인증 없이 접근 가능
                
                ### 주요 기능
                1. **회원 관리**
                   - 회원가입 (인증 불필요)
                   - 회원 목록 조회 (관리자 전용)
                   - 회원 상세 조회 (관리자 전용)
                
                2. **관리자 기능**
                   - 전체 회원 관리
                   - 페이징 및 정렬 지원
                   - 민감정보 마스킹 처리
                   - 메시지 발송 관리 (카카오톡/SMS)
                
                3. **테스트 API (H2 메모리 DB 전용)**
                   - 랜덤 유저 생성 (연령대 지정 가능)
                   - 모든 유저 삭제
                   - 테스트 데이터 관리
                
                ### 보안 정책
                - Spring Security 6.x 기반 보안 설정
                - BCrypt 패스워드 인코딩
                - CSRF 비활성화 (REST API)
                - 세션 기반 인증 지원
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("AutoEver Development Team")
                .email("dev@autoever.com")
                .url("https://autoever.com")
            )
            .license(new License()
                .name("Apache License 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0")
            );
    }
}