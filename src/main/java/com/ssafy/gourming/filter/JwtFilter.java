package com.ssafy.gourming.filter;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ssafy.gourming.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security 필터 체인에서 매 요청마다 한 번씩 실행되는 커스텀 JWT 검증 필터입니다.
 * HTTP 요청의 Authorization 헤더에서 토큰을 추출하고, 토큰이 유효한 경우 
 * Spring SecurityContext에 인증 정보(Authentication)를 세팅합니다.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * 필터 체인 내부에서 실제 필터링 로직을 수행합니다.
     * 1. Request에서 JWT 파싱
     * 2. JWT 유효성 검증
     * 3. 통과 시 SecurityContextHolder에 인증 객체(UsernamePasswordAuthenticationToken) 저장
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                // 토큰이 정상일 경우 이메일을 추출하여 인증 주체(Principal)로 설정합니다.
                String email = jwtUtil.getEmailFromToken(jwt);

                // Authentication 객체 생성 (비밀번호는 null, 권한은 빈 리스트)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, null, new ArrayList<>());
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 현재 스레드의 SecurityContext에 인증 정보 등록 (이후 컨트롤러 등에서 참조 가능)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 인증 객체를 세팅할 수 없는 경우 에러 로그 기록 후 통과시킴 (SecurityConfig 규칙에 따라 거부될 수 있음)
            logger.error("Cannot set user authentication: {}", e);
        }

        // 다음 필터로 요청을 전달합니다.
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 "Authorization" 값을 찾아 "Bearer " 접두사를 제거한 순수 JWT 문자열을 반환합니다.
     * 
     * @param request HTTP 서블릿 요청 객체
     * @return "Bearer " 접두사가 제거된 토큰, 없으면 null
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        // 헤더가 존재하고 "Bearer "로 시작하면 그 뒤의 문자열 반환
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
