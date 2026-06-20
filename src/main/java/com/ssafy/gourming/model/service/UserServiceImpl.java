package com.ssafy.gourming.model.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.mapper.UserMapper;

import com.ssafy.gourming.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final GroupService groupService;
	private final ImageService imageService;
	
	@Override
	@Transactional
	public void signup(UserDto.SignupRequest request) {
		// 1. 이메일 중복 체크
		if (userMapper.findByEmail(request.getEmail()) != null) {
			throw new IllegalArgumentException("Already Exists Email");
		}

		// 2. 핸들 중복 체크
		if (userMapper.findByHandle(request.getHandle()) != null) {
			throw new IllegalArgumentException("Already Exists Handle");
		}

		// 3. 비밀번호 Bcrypt 암호화 후 request 내 password 교체
		request.setPassword(passwordEncoder.encode(request.getPassword()));

		// 4. DB INSERT
		userMapper.insertUser(request);

		// DB에서 생성된 사용자 ID로 기본 그룹을 생성한다.
		UserDto.UserEntity createdUser = userMapper.findByEmail(request.getEmail());
		if (createdUser == null) {
			throw new IllegalStateException("Failed to find created user");
		}
		groupService.createDefaultGroup(createdUser.getId());
	}

	@Override
	public UserDto.LoginResponse login(UserDto.LoginRequest request) {
		// 1. 이메일로 사용자 조회
		UserDto.UserEntity user = userMapper.findByEmail(request.getEmail());
		
		// 2. 사용자가 없거나 비밀번호 불일치 -> 동일 메시지로 예외처리
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new org.springframework.security.authentication.BadCredentialsException("Invalid Email or Password");
		}
		
		// 3. JWT 생성 후 응답 반환 (프로필 정보 전체를 Body에 포함)
		String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole());
		return new UserDto.LoginResponse(
				token, 
				user.getId(), 
				user.getNickname(),
				user.getEmail(),
				user.getHandle(), 
				user.getProfileImage(),
				user.getRole()
			);
	}

	/**
	 * 주어진 핸들(닉네임 ID 역할)이 사용 가능한지 확인합니다.
	 * 
	 * @param handle 중복 검사할 핸들 문자열
	 * @return 사용 가능하면 true (DB에 존재하지 않음), 이미 사용 중이면 false 반환
	 */
	@Override
	public boolean isHandleAvailable(String handle) {
		return !userMapper.existsByHandle(handle);
	}

	@Override
	public UserDto.UserProfileResponse getUserProfile(String handle, String authenticatedUserId) {
		String currentUserId = null;
		// SecurityContext에서 얻어온 주체가 익명 사용자(anonymousUser)가 아닐 경우 식별자를 매핑합니다.
		if (authenticatedUserId != null && !authenticatedUserId.equals("anonymousUser")) {
			currentUserId = authenticatedUserId;
		}
		
		UserDto.UserProfileResponse profile = userMapper.getUserProfileWithStats(handle, currentUserId);
		if(profile == null) {
			throw new NoSuchElementException("User not found: " + handle);
		}
		return profile;
	}

	@Override
	public java.util.List<UserDto.UserProfileResponse> searchUsers(String keyword, String authenticatedUserId, int limit, int offset) {
		String currentUserId = null;
		// 검색을 요청한 현재 인증 사용자가 있을 경우, 목록 데이터와 함께 팔로우 상태를 도출할 수 있도록 식별자를 준비합니다.
		if (authenticatedUserId != null && !authenticatedUserId.equals("anonymousUser")) {
			currentUserId = authenticatedUserId;
		}
		return userMapper.searchUsers(keyword, currentUserId, limit, offset);
	}

	/**
	 * 사용자의 프로필 정보를 수정합니다.
	 * 수정 시 권한(본인 여부) 검증과 핸들 중복 검사를 수행합니다.
	 * 
	 * @param targetUserId 수정하려는 대상 사용자의 식별자(UUID)
	 * @param authenticatedUserId SecurityContext에서 가져온 현재 인증된 사용자의 ID
	 * @param request 변경할 프로필 데이터(닉네임, 핸들, 이미지, 소개 등)
	 * @throws SecurityException 본인의 프로필이 아닐 경우 예외 발생
	 * @throws IllegalArgumentException 변경하려는 핸들이 이미 타인에 의해 사용 중인 경우 예외 발생
	 */
	@Override
	public void updateProfile(
			String targetUserId,
			String authenticatedUserId,
			UserDto.UpdateProfileRequest request
	) {
		// 1. 토큰의 사용자 ID와 수정 대상 ID를 비교해 본인 요청인지 확인합니다.
		if (!Objects.equals(authenticatedUserId, targetUserId)) {
			throw new SecurityException("자신의 프로필만 수정할 수 있습니다.");
		}

		UserDto.UserEntity authUser = userMapper.findById(authenticatedUserId);
		if (authUser == null) {
			throw new NoSuchElementException("User not found: " + authenticatedUserId);
		}

		// 2. 핸들 중복 체크: 사용자가 핸들을 변경하려고 할 때, 해당 핸들이 이미 존재하는지 확인합니다.
		// (단, 기존에 본인이 사용 중인 핸들을 그대로 유지하는 경우는 허용합니다.)
		UserDto.UserEntity existingHandleUser = userMapper.findByHandle(request.getHandle());
		if (existingHandleUser != null && !existingHandleUser.getId().equals(targetUserId)) {
			throw new IllegalArgumentException("Already Exists Handle");
		}
		
		// 3. 이미지 동기화: 새 이미지가 설정된 경우 상태 업데이트
		String oldImage = authUser.getProfileImage();
		String newImage = request.getProfileImage();
		
		if (newImage != null && !newImage.equals(oldImage)) {
			String[] oldUrls = oldImage != null ? new String[]{oldImage} : new String[]{};
			String[] newUrls = new String[]{newImage};
			imageService.syncImages(oldUrls, newUrls);
		} else if (newImage == null && oldImage != null) {
			imageService.syncImages(new String[]{oldImage}, new String[]{});
		}

		// 4. 모든 검증을 통과하면 DB에 프로필 업데이트 쿼리를 실행합니다.
		try {
			userMapper.updateProfile(targetUserId, request);
		} catch (org.springframework.dao.DuplicateKeyException e) {
			throw new IllegalArgumentException("Already Exists Handle");
		}
	}
}
