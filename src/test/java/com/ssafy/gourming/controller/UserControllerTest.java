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
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String uid() { return String.valueOf(System.currentTimeMillis()); }

    @Test
    @DisplayName("[Controller] 회원가입 성공 → 201 Created")
    void signup_201() throws Exception {
        Map<String, String> body = Map.of(
            "email",    "s_" + uid() + "@test.com",
            "password", "password123!",
            "nickname", "신규유저",
            "handle",   "@new_" + uid()
        );
        log.info("회원가입 요청: {}", body.get("email"));

        mockMvc.perform(post("/api/user/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andDo(print());  // 요청/응답 전체 출력
    }

    @Test
    @DisplayName("[Controller] 중복 이메일 → 400 Bad Request")
    void signup_duplicate_400() throws Exception {
        String email  = "d_" + uid() + "@test.com";
        String handle = "@dup_" + uid();
        Map<String, String> body = Map.of(
            "email", email, "password", "pw", "nickname", "유저", "handle", handle);

        mockMvc.perform(post("/api/user/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());
        log.info("1차 가입 성공");

        mockMvc.perform(post("/api/user/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andDo(print());
        log.info("2차 가입(중복) → 400 확인");
    }

    @Test
    @DisplayName("[Controller] 로그인 성공 → 200 OK + token 포함")
    void login_200_withToken() throws Exception {
        String email  = "l_" + uid() + "@test.com";
        String handle = "@login_" + uid();

        Map<String, String> signupBody = Map.of(
            "email", email, "password", "pw123!", "nickname", "로그인유저", "handle", handle);
        mockMvc.perform(post("/api/user/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupBody)))
            .andExpect(status().isCreated());
        log.info("사전 가입 완료: {}", email);

        Map<String, String> loginBody = Map.of("email", email, "password", "pw123!");
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.nickname").isNotEmpty())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andDo(print());
    }

    @Test
    @DisplayName("[Controller] 잘못된 비밀번호 → 401 Unauthorized")
    void login_wrongPw_401() throws Exception {
        Map<String, String> body = Map.of(
            "email", "nobody@test.com", "password", "wrongPw!");
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized())
            .andDo(print());
    }

    @Test
    @DisplayName("[Controller] 프로필 조회 → 200 OK + 민감 필드 없음")
    void getProfile_200() throws Exception {
        String handle = "@p_" + uid();
        String email  = "p_" + uid() + "@test.com";

        Map<String, String> signupBody = Map.of(
            "email", email, "password", "pw", "nickname", "프로필유저", "handle", handle);
        mockMvc.perform(post("/api/user/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupBody)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/user/" + handle))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.handle").value(handle))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.phone").doesNotExist())
            .andExpect(jsonPath("$.email").doesNotExist())
            .andDo(print());
    }

    @Test
    @DisplayName("[Controller] 없는 handle → 404 Not Found")
    void getProfile_404() throws Exception {
        mockMvc.perform(get("/api/user/@nobody_ever_exists"))
            .andExpect(status().isNotFound())
            .andDo(print());
    }
}
