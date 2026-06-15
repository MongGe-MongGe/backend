package com.ssafy.gourming.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ssafy.gourming.filter.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable()) // JWT로 인증하므로 CSRTF 토큰이 불필요
			.cors(Customizer.withDefaults()) // WebConfig의 CORS 설정을 따름
			.sessionManagement(session -> 
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 서버에 JWT 세션 생성 금지
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api/auth/**").permitAll()              // 회원가입, 로그인 공개
					.requestMatchers(HttpMethod.GET, "/api/users/me/**").authenticated() // 내 프로필 정보 등 민감한 정보 보호 (우선순위 적용)
					.requestMatchers(HttpMethod.GET, "/api/users/**").permitAll() // 프로필 조회 등 GET 요청 공개
					// 프론트엔드에서 로컬 디스크의 이미지를 직접 렌더링하기 위해 정적 리소스 경로를 인증 없이 허용합니다.
					.requestMatchers("/images/**").permitAll() 
					.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger API 문서 공개
					.anyRequest().authenticated() // 그 외의 모든 요청은 인증 필요
			)
			// 인증 실패 시 403 Forbidden 대신 401 Unauthorized를 반환하도록 예외 처리 커스터마이징
			.exceptionHandling(exception -> exception
					.authenticationEntryPoint((request, response, authException) -> {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
					})
			)
			// 스프링 시큐리티의 기본 폼 로그인 필터(UsernamePasswordAuthenticationFilter)가 실행되기 전에
			// 커스텀 JwtFilter를 먼저 통과하도록 설정하여 토큰 인증을 선행 처리합니다.
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
}
