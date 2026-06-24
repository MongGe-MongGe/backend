package com.ssafy.gourming.model.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.gourming.model.dto.PasswordResetDto;
import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.mapper.PasswordResetTokenMapper;
import com.ssafy.gourming.model.mapper.UserMapper;
import com.ssafy.gourming.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock GroupService groupService;
    @Mock ImageService imageService;
    @Mock PasswordResetTokenMapper passwordResetTokenMapper;
    @Mock PasswordResetNotifier passwordResetNotifier;
    @InjectMocks UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "passwordResetExpirationMinutes", 30L);
        log.info(">>> Mock 설정 준비 완료");
    }

    @Test
    @DisplayName("[Service] 회원가입 성공")
    void signUp_success() {
        UserDto.SignupRequest req = makeSignupReq("new@email.com");
        UserDto.UserEntity createdUser = makeEntity("new@email.com", "$2a$encoded");
        when(userMapper.findByEmail("new@email.com")).thenReturn(null, createdUser);
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");
        log.info("Mock: findByEmail → null (중복 없음), encode → $2a$encoded");

        assertDoesNotThrow(() -> userService.signup(req));
        verify(userMapper, times(1)).insertUser(req);
        verify(groupService).createDefaultGroup("uuid-001");
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
        verify(groupService, never()).createDefaultGroup(anyString());
        log.info("✔ 예외 메시지: '{}', insertUser 미호출 확인", ex.getMessage());
    }

    @Test
    @DisplayName("[Service] 회원 저장 후 조회 실패 시 기본 그룹을 생성하지 않음")
    void signUp_createdUserNotFound() {
        UserDto.SignupRequest req = makeSignupReq("missing@email.com");
        when(userMapper.findByEmail("missing@email.com")).thenReturn(null);
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");

        IllegalStateException ex = assertThrows(
            IllegalStateException.class, () -> userService.signup(req));

        assertEquals("Failed to find created user", ex.getMessage());
        verify(userMapper).insertUser(req);
        verify(groupService, never()).createDefaultGroup(anyString());
    }

    @Test
    @DisplayName("[Service] 로그인 성공 - JWT 토큰 반환")
    void login_success() {
        UserDto.LoginRequest req = makeLoginReq("user@email.com", "plainPw");
        UserDto.UserEntity entity = makeEntity("user@email.com", "$2a$hashed");
        when(userMapper.findByEmail("user@email.com")).thenReturn(entity);
        when(passwordEncoder.matches("plainPw", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("user@email.com", "uuid-001", "USER")).thenReturn("mock.jwt.token");
        log.info("Mock: findByEmail → entity, matches → true, generateToken → mock");

        UserDto.LoginResponse res = userService.login(req);
        log.info("LoginResponse token: {}", res.getToken());

        assertNotNull(res.getToken());
        assertFalse(res.getToken().isEmpty());
        verify(jwtUtil).generateToken("user@email.com", "uuid-001", "USER");
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

    @Test
    @DisplayName("[Service] 비밀번호 재설정 요청 성공 - 토큰 저장 및 알림 호출")
    void requestPasswordReset_success() {
        PasswordResetDto.PasswordResetRequest req = makePasswordResetRequest("user@email.com");
        UserDto.UserEntity entity = makeEntity("user@email.com", "$2a$hashed");
        when(userMapper.findByEmail("user@email.com")).thenReturn(entity);

        LocalDateTime before = LocalDateTime.now();

        assertDoesNotThrow(() -> userService.requestPasswordReset(req));

        ArgumentCaptor<PasswordResetDto.PasswordResetTokenEntity> tokenCaptor =
            ArgumentCaptor.forClass(PasswordResetDto.PasswordResetTokenEntity.class);
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(passwordResetTokenMapper).deleteUnusedTokensByUserId("uuid-001");
        verify(passwordResetTokenMapper).insertToken(tokenCaptor.capture());
        verify(passwordResetNotifier).notifyPasswordReset(
            eq("user@email.com"),
            rawTokenCaptor.capture(),
            expiresAtCaptor.capture()
        );

        PasswordResetDto.PasswordResetTokenEntity savedToken = tokenCaptor.getValue();
        String rawToken = rawTokenCaptor.getValue();

        assertNotNull(savedToken.getId());
        assertEquals("uuid-001", savedToken.getUserId());
        assertNotNull(savedToken.getTokenHash());
        assertFalse(savedToken.getTokenHash().isBlank());
        assertNotEquals(rawToken, savedToken.getTokenHash());
        assertNull(savedToken.getUsedAt());
        assertNull(savedToken.getCreatedAt());
        assertTrue(savedToken.getExpiresAt().isAfter(before));
        assertEquals(savedToken.getExpiresAt(), expiresAtCaptor.getValue());
        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
    }

    @Test
    @DisplayName("[Service] 비밀번호 재설정 요청 - 없는 이메일도 조용히 성공 처리")
    void requestPasswordReset_userNotFound() {
        PasswordResetDto.PasswordResetRequest req = makePasswordResetRequest("ghost@email.com");
        when(userMapper.findByEmail("ghost@email.com")).thenReturn(null);

        assertDoesNotThrow(() -> userService.requestPasswordReset(req));

        verify(userMapper).findByEmail("ghost@email.com");
        verifyNoInteractions(passwordResetTokenMapper, passwordResetNotifier);
    }

    @Test
    @DisplayName("[Service] 비밀번호 재설정 확정 성공")
    void confirmPasswordReset_success() {
        PasswordResetDto.PasswordResetConfirmRequest req =
            makePasswordResetConfirmRequest("raw-reset-token", "newPassword123!");
        PasswordResetDto.PasswordResetTokenEntity token =
            makePasswordResetToken("token-id-1", "uuid-001");

        when(passwordResetTokenMapper.findValidTokenByHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(token);
        when(passwordEncoder.encode("newPassword123!")).thenReturn("$2a$encodedNewPassword");
        when(userMapper.updatePassword("uuid-001", "$2a$encodedNewPassword")).thenReturn(1);
        when(passwordResetTokenMapper.markTokenUsed(eq("token-id-1"), any(LocalDateTime.class)))
            .thenReturn(1);

        assertDoesNotThrow(() -> userService.confirmPasswordReset(req));

        verify(passwordResetTokenMapper).findValidTokenByHash(anyString(), any(LocalDateTime.class));
        verify(passwordEncoder).encode("newPassword123!");
        verify(userMapper).updatePassword("uuid-001", "$2a$encodedNewPassword");
        verify(passwordResetTokenMapper).markTokenUsed(eq("token-id-1"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("[Service] 비밀번호 재설정 확정 실패 - 유효하지 않은 토큰")
    void confirmPasswordReset_invalidToken() {
        PasswordResetDto.PasswordResetConfirmRequest req =
            makePasswordResetConfirmRequest("invalid-token", "newPassword123!");
        when(passwordResetTokenMapper.findValidTokenByHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(null);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> userService.confirmPasswordReset(req)
        );

        assertEquals("Invalid or expired password reset token", ex.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).updatePassword(anyString(), anyString());
        verify(passwordResetTokenMapper, never()).markTokenUsed(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("[Service] 비밀번호 재설정 확정 실패 - 비밀번호 업데이트 실패")
    void confirmPasswordReset_updatePasswordFailed() {
        PasswordResetDto.PasswordResetConfirmRequest req =
            makePasswordResetConfirmRequest("raw-reset-token", "newPassword123!");
        PasswordResetDto.PasswordResetTokenEntity token =
            makePasswordResetToken("token-id-1", "uuid-001");

        when(passwordResetTokenMapper.findValidTokenByHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(token);
        when(passwordEncoder.encode("newPassword123!")).thenReturn("$2a$encodedNewPassword");
        when(userMapper.updatePassword("uuid-001", "$2a$encodedNewPassword")).thenReturn(0);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> userService.confirmPasswordReset(req)
        );

        assertEquals("Failed to update password", ex.getMessage());
        verify(passwordResetTokenMapper, never()).markTokenUsed(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("[Service] 비밀번호 재설정 확정 실패 - 토큰 사용 처리 실패")
    void confirmPasswordReset_markTokenUsedFailed() {
        PasswordResetDto.PasswordResetConfirmRequest req =
            makePasswordResetConfirmRequest("raw-reset-token", "newPassword123!");
        PasswordResetDto.PasswordResetTokenEntity token =
            makePasswordResetToken("token-id-1", "uuid-001");

        when(passwordResetTokenMapper.findValidTokenByHash(anyString(), any(LocalDateTime.class)))
            .thenReturn(token);
        when(passwordEncoder.encode("newPassword123!")).thenReturn("$2a$encodedNewPassword");
        when(userMapper.updatePassword("uuid-001", "$2a$encodedNewPassword")).thenReturn(1);
        when(passwordResetTokenMapper.markTokenUsed(eq("token-id-1"), any(LocalDateTime.class)))
            .thenReturn(0);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> userService.confirmPasswordReset(req)
        );

        assertEquals("Failed to mark password reset token as used", ex.getMessage());
    }

    @Test
    @DisplayName("[Service] 본인 프로필 수정 성공")
    void updateProfile_success() {
        UserDto.UpdateProfileRequest request =
            new UserDto.UpdateProfileRequest("새닉네임", "@newhandle", "new-image", "소개");
        UserDto.UserEntity authUser = makeEntity("user@email.com", "$2a$hashed");
        ReflectionTestUtils.setField(authUser, "profileImage", "old-image");

        when(userMapper.findById("uuid-001")).thenReturn(authUser);
        when(userMapper.findByHandle("@newhandle")).thenReturn(null);

        assertDoesNotThrow(() -> userService.updateProfile("uuid-001", "uuid-001", request));

        verify(userMapper).findById("uuid-001");
        verify(userMapper, never()).findByEmail(anyString());
        verify(imageService).syncImages(any(String[].class), any(String[].class));
        verify(userMapper).updateProfile("uuid-001", request);
    }

    @Test
    @DisplayName("[Service] 다른 사용자의 프로필 수정 차단")
    void updateProfile_forbidden() {
        UserDto.UpdateProfileRequest request =
            new UserDto.UpdateProfileRequest("새닉네임", "@newhandle", null, "소개");

        SecurityException ex = assertThrows(
            SecurityException.class,
            () -> userService.updateProfile("user-2", "user-1", request)
        );

        assertEquals("자신의 프로필만 수정할 수 있습니다.", ex.getMessage());
        verify(userMapper, never()).findById(anyString());
        verify(userMapper, never()).updateProfile(anyString(), any());
    }

    @Test
    @DisplayName("[Service] 존재하지 않는 사용자 프로필 수정 실패")
    void updateProfile_userNotFound() {
        UserDto.UpdateProfileRequest request =
            new UserDto.UpdateProfileRequest("새닉네임", "@newhandle", null, "소개");
        when(userMapper.findById("user-1")).thenReturn(null);

        NoSuchElementException ex = assertThrows(
            NoSuchElementException.class,
            () -> userService.updateProfile("user-1", "user-1", request)
        );

        assertEquals("User not found: user-1", ex.getMessage());
        verify(userMapper, never()).updateProfile(anyString(), any());
    }

    @Test
    @DisplayName("[Service] 프로필 조회 성공")
    void getUserProfile_success() {
        UserDto.UserProfileResponse mockResponse = new UserDto.UserProfileResponse();
        mockResponse.setHandle("@tester");
        when(userMapper.getUserProfileWithStats("@tester", "user-1")).thenReturn(mockResponse);

        UserDto.UserProfileResponse response = userService.getUserProfile("@tester", "user-1");

        assertNotNull(response);
        assertEquals("@tester", response.getHandle());
        verify(userMapper).getUserProfileWithStats("@tester", "user-1");
    }

    @Test
    @DisplayName("[Service] 프로필 조회 실패 - 사용자 없음")
    void getUserProfile_notFound() {
        when(userMapper.getUserProfileWithStats("@ghost", "user-1")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> userService.getUserProfile("@ghost", "user-1"));
    }

    @Test
    @DisplayName("[Service] 유저 검색 성공")
    void searchUsers_success() {
        UserDto.UserProfileResponse mockResponse = new UserDto.UserProfileResponse();
        mockResponse.setNickname("테스터");
        when(userMapper.searchUsers("테스트", "user-1", 20, 0))
            .thenReturn(java.util.List.of(mockResponse));

        java.util.List<UserDto.UserProfileResponse> result = userService.searchUsers("테스트", "user-1", 20, 0);

        assertEquals(1, result.size());
        assertEquals("테스터", result.get(0).getNickname());
        verify(userMapper).searchUsers("테스트", "user-1", 20, 0);
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

    private PasswordResetDto.PasswordResetRequest makePasswordResetRequest(String email) {
        PasswordResetDto.PasswordResetRequest r = new PasswordResetDto.PasswordResetRequest();
        ReflectionTestUtils.setField(r, "email", email);
        return r;
    }

    private PasswordResetDto.PasswordResetConfirmRequest makePasswordResetConfirmRequest(
        String token,
        String newPassword
    ) {
        PasswordResetDto.PasswordResetConfirmRequest r =
            new PasswordResetDto.PasswordResetConfirmRequest();
        ReflectionTestUtils.setField(r, "token", token);
        ReflectionTestUtils.setField(r, "newPassword", newPassword);
        return r;
    }

    private PasswordResetDto.PasswordResetTokenEntity makePasswordResetToken(String id, String userId) {
        PasswordResetDto.PasswordResetTokenEntity token =
            new PasswordResetDto.PasswordResetTokenEntity();
        token.setId(id);
        token.setUserId(userId);
        token.setTokenHash("hashed-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return token;
    }

    private UserDto.UserEntity makeEntity(String email, String encodedPw) {
        UserDto.UserEntity e = new UserDto.UserEntity();
        ReflectionTestUtils.setField(e, "id",       "uuid-001");
        ReflectionTestUtils.setField(e, "email",    email);
        ReflectionTestUtils.setField(e, "password", encodedPw);
        ReflectionTestUtils.setField(e, "nickname", "테스터");
        ReflectionTestUtils.setField(e, "handle",   "@tester");
        ReflectionTestUtils.setField(e, "role",     "USER");
        return e;
    }
}
