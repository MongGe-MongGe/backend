package com.ssafy.gourming.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil,
                "jwtSecret",
                "test-secret-key-must-be-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 60_000L);
    }

    @Test
    void generateTokenStoresEmailAndUserId() {
        String token = jwtUtil.generateToken("user@email.com", "user-1");

        assertEquals("user@email.com", jwtUtil.getEmailFromToken(token));
        assertEquals("user-1", jwtUtil.getUserIdFromToken(token));
        assertTrue(jwtUtil.validateToken(token));
    }
}
