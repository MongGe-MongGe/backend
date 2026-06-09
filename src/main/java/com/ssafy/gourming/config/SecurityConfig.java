package com.ssafy.gourming.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable()) // JWT로 인증하므로 CSRTF 토큰이 불필요
			.cors(Customizer.withDefaults()) // WebConfig의 CORS 설정을 따름
			.sessionManagement(session -> 
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 서버에 JWT 세션 생성 금지
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api/auth/**").permitAll()              // 회원가입, 로그인 공개
					.requestMatchers("/api/users/me/**").authenticated()
					.requestMatchers(HttpMethod.GET, "/api/users/**").permitAll() // 프로필 조회 공개
					.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
					.anyRequest().authenticated()
			);
		
		return http.build();
	}
}
