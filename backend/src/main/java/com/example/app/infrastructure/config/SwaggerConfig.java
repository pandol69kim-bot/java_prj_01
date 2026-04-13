package com.example.app.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "BearerAuth";

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("로컬 개발 서버"),
                        new Server().url("https://api.example.com").description("운영 서버")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, jwtSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("외부 연동 통합 관리 시스템 API")
                .version("v1.0.0")
                .description("""
                        외부 시스템과의 데이터 연동 및 동기화를 관리하는 REST API입니다.

                        ## 인증
                        모든 API는 JWT Bearer 토큰 인증이 필요합니다.
                        `POST /api/v1/auth/login` 으로 토큰을 발급받아 사용하세요.

                        ## 공통 응답 형식
                        ```json
                        {
                          "success": true,
                          "data": { ... },
                          "error": null
                        }
                        ```
                        """)
                .contact(new Contact()
                        .name("개발팀")
                        .email("dev@example.com"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Access Token을 입력하세요. (Bearer 접두사 불필요)");
    }
}
