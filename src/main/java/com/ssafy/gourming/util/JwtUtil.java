package com.ssafy.gourming.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT(Json Web Token) 생성 및 검증을 담당하는 유틸리티 클래스입니다.
 * JWT 서명 키(Secret)와 만료 시간(Expiration)은 application.properties에서 주입받습니다.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * application.properties에 설정된 jwt.secret 값을 바이트 배열로 변환하여
     * HMAC-SHA 알고리즘에 적합한 SecretKey 객체를 생성합니다.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 인증된 사용자(이메일)를 기반으로 JWT 토큰을 생성하여 반환합니다.
     * 
     * @param email 토큰의 Subject(주체)로 들어갈 사용자의 이메일
     * @return 서명된 JWT 문자열
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰의 서명을 검증하고, Payload(Claims) 영역에서 Subject(이메일) 값을 추출합니다.
     * 
     * @param token 파싱할 JWT 문자열
     * @return 토큰에 저장된 이메일 문자열
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * 토큰의 유효성을 검증합니다. (서명 일치 여부, 만료 여부 등)
     * 
     * @param token 검증할 JWT 문자열
     * @return 유효한 토큰일 경우 true, 만료되거나 변조된 경우 false 반환
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // 서명 불일치, 만료된 토큰, 잘못된 형식 등인 경우 false 처리
            return false;
        }
    }
}
