package com.ssafy.gourming.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.mapper.GroupMapper;
import com.ssafy.gourming.model.mapper.UserMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserMapper userMapper;
    @Autowired GroupMapper groupMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    private String uid() { return String.valueOf(System.currentTimeMillis()); }

    @Test
    @DisplayName("[Auth] 회원가입 성공 → 201 Created")
    void signup_201() throws Exception {
        String email = "s_" + uid() + "@test.com";
        Map<String, String> body = Map.of(
            "email",    email,
            "password", "password123!",
            "nickname", "신규유저",
            "handle",   "@new_" + uid()
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andDo(print());

        UserDto.UserEntity createdUser = userMapper.findByEmail(email);
        assertNotNull(createdUser);
        assertNotNull(groupMapper.selectDefaultGroupByUserId(createdUser.getId()));
    }

    @Test
    @DisplayName("[Auth] 중복 이메일 → 400 Bad Request")
    void signup_duplicate_400() throws Exception {
        String email  = "d_" + uid() + "@test.com";
        String handle = "@dup_" + uid();
        Map<String, String> body = Map.of(
            "email", email, "password", "password123!", "nickname", "유저", "handle", handle);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andDo(print());
    }

    @Test
    @DisplayName("[Auth] 로그인 성공 → 200 OK + token 포함")
    void login_200_withToken() throws Exception {
        String email  = "l_" + uid() + "@test.com";
        String handle = "@login_" + uid();

        Map<String, String> signupBody = Map.of(
            "email", email, "password", "password123!", "nickname", "로그인유저", "handle", handle);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupBody)))
            .andExpect(status().isCreated());

        Map<String, String> loginBody = Map.of("email", email, "password", "password123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.nickname").isNotEmpty())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andDo(print());
    }

    @Test
    @DisplayName("[Auth] 잘못된 비밀번호 → 401 Unauthorized")
    void login_wrongPw_401() throws Exception {
        Map<String, String> body = Map.of(
            "email", "nobody@test.com", "password", "wrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized())
            .andDo(print());
    }

    @Test
    @DisplayName("[Auth] 비밀번호 재설정 요청 성공")
    void passwordResetRequest_200() throws Exception {
        String email = "reset_" + uid() + "@test.com";
        String handle = "@reset_" + uid();
        Map<String, String> signupBody = Map.of(
            "email", email, "password", "password123!", "nickname", "재설정유저", "handle", handle);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupBody)))
            .andExpect(status().isCreated());

        Map<String, String> body = Map.of("email", email);
        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(content().string("비밀번호 재설정 안내를 이메일로 발송했습니다."))
            .andDo(print());

        UserDto.UserEntity user = userMapper.findByEmail(email);
        assertNotNull(user);
        Integer tokenCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM password_reset_tokens WHERE user_id = ? AND used_at IS NULL",
            Integer.class,
            user.getId()
        );
        assertNotNull(tokenCount);
        org.junit.jupiter.api.Assertions.assertTrue(tokenCount > 0);
    }

    @Test
    @DisplayName("[Auth] 존재하지 않는 이메일도 비밀번호 재설정 요청은 200")
    void passwordResetRequest_unknownEmail_200() throws Exception {
        Map<String, String> body = Map.of("email", "ghost_" + uid() + "@test.com");

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(content().string("비밀번호 재설정 안내를 이메일로 발송했습니다."))
            .andDo(print());
    }

    @Test
    @DisplayName("[Auth] 비밀번호 재설정 요청 이메일 형식 오류")
    void passwordResetRequest_invalidEmail_400() throws Exception {
        Map<String, String> body = Map.of("email", "invalid-email");

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andDo(print());
    }

    @Test
    @DisplayName("[Auth] 비밀번호 재설정 확정 성공")
    void passwordResetConfirm_200() throws Exception {
        String email = "confirm_" + uid() + "@test.com";
        String handle = "@cf_" + uid().substring(uid().length() - 8);
        String rawToken = "raw-confirm-token-" + uid();
        Map<String, String> signupBody = Map.of(
            "email", email, "password", "password123!", "nickname", "확정유저", "handle", handle);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupBody)))
            .andExpect(status().isCreated());

        UserDto.UserEntity user = userMapper.findByEmail(email);
        assertNotNull(user);
        insertPasswordResetToken(user.getId(), rawToken, LocalDateTime.now().plusMinutes(30));

        Map<String, String> confirmBody = Map.of(
            "token", rawToken,
            "newPassword", "newPassword123!"
        );
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmBody)))
            .andExpect(status().isOk())
            .andExpect(content().string("비밀번호가 변경되었습니다."))
            .andDo(print());

        Map<String, String> newLoginBody = Map.of("email", email, "password", "newPassword123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLoginBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        Map<String, String> oldLoginBody = Map.of("email", email, "password", "password123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oldLoginBody)))
            .andExpect(status().isUnauthorized());

        Integer unusedTokenCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM password_reset_tokens WHERE user_id = ? AND used_at IS NULL",
            Integer.class,
            user.getId()
        );
        assertNotNull(unusedTokenCount);
        org.junit.jupiter.api.Assertions.assertEquals(0, unusedTokenCount);
    }

    @Test
    @DisplayName("[Auth] 비밀번호 재설정 확정 실패 - 잘못된 토큰")
    void passwordResetConfirm_invalidToken_400() throws Exception {
        Map<String, String> body = Map.of(
            "token", "invalid-token",
            "newPassword", "newPassword123!"
        );

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andDo(print());
    }

    @Test
    @DisplayName("[Auth] 비밀번호 재설정 확정 실패 - 짧은 비밀번호")
    void passwordResetConfirm_shortPassword_400() throws Exception {
        Map<String, String> body = Map.of(
            "token", "some-token",
            "newPassword", "short"
        );

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andDo(print());
    }

    private void insertPasswordResetToken(String userId, String rawToken, LocalDateTime expiresAt) {
        jdbcTemplate.update(
            """
            INSERT INTO password_reset_tokens (
                id,
                user_id,
                token_hash,
                expires_at
            )
            VALUES (?, ?, ?, ?)
            """,
            UUID.randomUUID().toString(),
            userId,
            hashToken(rawToken),
            expiresAt
        );
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash test token", e);
        }
    }
}
