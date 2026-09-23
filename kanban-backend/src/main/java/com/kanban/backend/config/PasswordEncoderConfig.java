package com.kanban.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SecurityConfig에 같이 두면 순환 참조가 생김: SecurityConfig -> OAuth2AuthenticationSuccessHandler
 * -> AuthService -> PasswordEncoder(SecurityConfig의 @Bean) -> SecurityConfig 자기 자신.
 * 그래서 별도 설정 클래스로 분리.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
