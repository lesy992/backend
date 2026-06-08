package com.sum.backend.confige;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // Security 스키마 이름 (임의로 지정)
        String jwtSchemeName = "jwtAuth";

        // API 요청 헤더에 인증 정보 포함 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        // Security 스키마 설정
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer") // Bearer 토큰 방식
                        .bearerFormat("JWT")); // 토큰 포맷 명시

        return new OpenAPI()
                .info(new Info()
                        .title("Sum API 문서")
                        .description("회원가입, 로그인 및 JWT 인증이 적용된 API 명세서입니다.")
                        .version("1.0.0"))
                .addSecurityItem(securityRequirement) // 전역으로 SecurityRequirement 적용
                .components(components);
    }
}
