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

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";

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
     * 이메일을 Subject로 유지하고 사용자 ID와 Role을 별도 Claim에 저장한 JWT를 생성합니다.
     *
     * @param email 토큰의 Subject로 들어갈 사용자의 이메일
     * @param userId 인증 Principal로 사용할 사용자의 ID
     * @param role 사용자의 권한
     * @return 서명된 JWT 문자열
     */
    public String generateToken(String email, String userId, String role) {
        return Jwts.builder()
                .subject(email)
                .claim(USER_ID_CLAIM, userId)
                .claim(ROLE_CLAIM, role)
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
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰의 서명을 검증하고 사용자 ID Claim을 추출합니다.
     *
     * @param token 파싱할 JWT 문자열
     * @return 토큰에 저장된 사용자 ID
     */
    public String getUserIdFromToken(String token) {
        return parseClaims(token).get(USER_ID_CLAIM, String.class);
    }

    /**
     * 토큰의 서명을 검증하고 사용자 Role Claim을 추출합니다.
     *
     * @param token 파싱할 JWT 문자열
     * @return 토큰에 저장된 사용자 권한
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    /**
     * 토큰의 유효성을 검증합니다. (서명 일치 여부, 만료 여부 등)
     * 
     * @param token 검증할 JWT 문자열
     * @return 유효한 토큰일 경우 true, 만료되거나 변조된 경우 false 반환
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            // 서명 불일치, 만료된 토큰, 잘못된 형식 등인 경우 false 처리
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
