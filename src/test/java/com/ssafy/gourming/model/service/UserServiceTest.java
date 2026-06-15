package com.ssafy.gourming.model.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.mapper.UserMapper;
import com.ssafy.gourming.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        log.info(">>> Mock 설정 준비 완료");
    }

    @Test
    @DisplayName("[Service] 회원가입 성공")
    void signUp_success() {
        UserDto.SignupRequest req = makeSignupReq("new@email.com");
        when(userMapper.findByEmail("new@email.com")).thenReturn(null);
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");
        log.info("Mock: findByEmail → null (중복 없음), encode → $2a$encoded");

        assertDoesNotThrow(() -> userService.signup(req));
        verify(userMapper, times(1)).insertUser(req);
        log.info("✔ insertUser 1회 호출 확인");
    }

    @Test
    @DisplayName("[Service] 중복 이메일 → IllegalArgumentException")
    void signUp_duplicateEmail() {
        UserDto.SignupRequest req = makeSignupReq("dup@email.com");
        when(userMapper.findByEmail("dup@email.com"))
            .thenReturn(new UserDto.UserEntity());
        log.info("Mock: findByEmail → 기존 유저 반환 (중복)");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class, () -> userService.signup(req));
        assertEquals("Already Exists Email", ex.getMessage());
        verify(userMapper, never()).insertUser(any());
        log.info("✔ 예외 메시지: '{}', insertUser 미호출 확인", ex.getMessage());
    }

    @Test
    @DisplayName("[Service] 로그인 성공 - JWT 토큰 반환")
    void login_success() {
        UserDto.LoginRequest req = makeLoginReq("user@email.com", "plainPw");
        UserDto.UserEntity entity = makeEntity("user@email.com", "$2a$hashed");
        when(userMapper.findByEmail("user@email.com")).thenReturn(entity);
        when(passwordEncoder.matches("plainPw", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("user@email.com")).thenReturn("mock.jwt.token");
        log.info("Mock: findByEmail → entity, matches → true, generateToken → mock");

        UserDto.LoginResponse res = userService.login(req);
        log.info("LoginResponse token: {}", res.getToken());

        assertNotNull(res.getToken());
        assertFalse(res.getToken().isEmpty());
        log.info("✔ JWT 토큰 정상 발급");
    }

    @Test
    @DisplayName("[Service] 로그인 실패 - 없는 이메일")
    void login_userNotFound() {
        UserDto.LoginRequest req = makeLoginReq("ghost@email.com", "pw");
        when(userMapper.findByEmail("ghost@email.com")).thenReturn(null);
        log.info("Mock: findByEmail → null");

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> userService.login(req));
        log.info("✔ BadCredentialsException 발생");
    }

    @Test
    @DisplayName("[Service] 로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword() {
        UserDto.LoginRequest req = makeLoginReq("user@email.com", "wrongPw");
        UserDto.UserEntity entity = makeEntity("user@email.com", "$2a$hashed");
        when(userMapper.findByEmail("user@email.com")).thenReturn(entity);
        when(passwordEncoder.matches("wrongPw", "$2a$hashed")).thenReturn(false);
        log.info("Mock: matches → false (비밀번호 불일치)");

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> userService.login(req));
        log.info("✔ BadCredentialsException 발생");
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────

    private UserDto.SignupRequest makeSignupReq(String email) {
        UserDto.SignupRequest r = new UserDto.SignupRequest();
        ReflectionTestUtils.setField(r, "email",    email);
        ReflectionTestUtils.setField(r, "password", "rawPw");
        ReflectionTestUtils.setField(r, "nickname", "테스터");
        ReflectionTestUtils.setField(r, "handle",   "@tester");
        return r;
    }

    private UserDto.LoginRequest makeLoginReq(String email, String pw) {
        UserDto.LoginRequest r = new UserDto.LoginRequest();
        ReflectionTestUtils.setField(r, "email",    email);
        ReflectionTestUtils.setField(r, "password", pw);
        return r;
    }

    private UserDto.UserEntity makeEntity(String email, String encodedPw) {
        UserDto.UserEntity e = new UserDto.UserEntity();
        ReflectionTestUtils.setField(e, "id",       "uuid-001");
        ReflectionTestUtils.setField(e, "email",    email);
        ReflectionTestUtils.setField(e, "password", encodedPw);
        ReflectionTestUtils.setField(e, "nickname", "테스터");
        ReflectionTestUtils.setField(e, "handle",   "@tester");
        return e;
    }
}
