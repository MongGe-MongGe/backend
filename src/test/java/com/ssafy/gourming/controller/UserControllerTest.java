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
    @DisplayName("[User] 프로필 조회 성공 → 200 OK + 민감 필드 없음")
    void getProfile_200() throws Exception {
        String handle = "@p_" + uid();
        String email  = "p_" + uid() + "@test.com";

        // 사전 가입 (/api/auth/signup 사용)
        Map<String, String> signupBody = Map.of(
            "email", email, "password", "pw", "nickname", "프로필유저", "handle", handle);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupBody)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/" + handle))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.handle").value(handle))
            .andExpect(jsonPath("$.nickname").isNotEmpty())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.phone").doesNotExist())
            .andExpect(jsonPath("$.email").doesNotExist())
            .andDo(print());
    }

    @Test
    @DisplayName("[User] 없는 handle → 404 Not Found")
    void getProfile_404() throws Exception {
        mockMvc.perform(get("/api/users/@nobody_ever_exists"))
            .andExpect(status().isNotFound())
            .andDo(print());
    }
}
