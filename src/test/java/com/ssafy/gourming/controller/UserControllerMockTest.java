package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.service.FollowService;
import com.ssafy.gourming.model.service.UserService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("사용자 컨트롤러 단위 테스트")
class UserControllerMockTest {

    private static final String USER_ID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private com.ssafy.gourming.model.mapper.UserMapper userMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("인증된 사용자 ID로 프로필을 수정한다")
    void updateProfile() throws Exception {
        UserDto.UpdateProfileRequest request =
                new UserDto.UpdateProfileRequest("새닉네임", "@new_handle", null, "소개");

        mockMvc.perform(put("/api/users/{id}", USER_ID)
                .with(authentication(new UsernamePasswordAuthenticationToken(
                        USER_ID, null, java.util.List.of())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userService).updateProfile(
                eq(USER_ID),
                eq(USER_ID),
                any(UserDto.UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("인증 없이 프로필을 수정할 수 없다")
    void updateProfileWithoutAuthenticationFails() throws Exception {
        UserDto.UpdateProfileRequest request =
                new UserDto.UpdateProfileRequest("새닉네임", "@new_handle", null, "소개");

        mockMvc.perform(put("/api/users/{id}", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(userService, never()).updateProfile(any(), any(), any());
    }
}
