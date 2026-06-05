package com.ssafy.gourming.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String uid() { return String.valueOf(System.currentTimeMillis()); }

    @Test
    @DisplayName("[Auth] 회원가입 성공 → 201 Created")
    void signup_201() throws Exception {
        Map<String, String> body = Map.of(
            "email",    "s_" + uid() + "@test.com",
            "password", "password123!",
            "nickname", "신규유저",
            "handle",   "@new_" + uid()
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andDo(print());
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
}
