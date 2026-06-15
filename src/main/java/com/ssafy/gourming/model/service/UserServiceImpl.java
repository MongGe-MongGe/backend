package com.ssafy.gourming.model.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.NoSuchElementException;

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
		String token = jwtUtil.generateToken(user.getEmail());
		return new UserDto.LoginResponse(
				token, 
				user.getId(), 
				user.getNickname(),
				user.getEmail(),
				user.getHandle(), 
				user.getProfileImage()
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
	public UserDto.UserProfileResponse getUserProfile(String handle) {
		// 1. handle로 사용자 조회 (내부 전용 UserEntity로 받음)
		UserDto.UserEntity user = userMapper.findByHandle(handle);
		
		// 2. 존재하지 않는 handle이면 예외처리
		if(user == null) {
			throw new NoSuchElementException("User not found: " + handle);
		}
		
		// 3. 민감 필드(password, phone, email)를 제외하고 안전 필드만 UserProfileResponse로 변환
		return new UserDto.UserProfileResponse(
				user.getId(), 
				user.getNickname(), 
				user.getHandle(), 
				user.getProfileImage(), 
				user.getBio()
			);
	}

	/**
	 * 사용자의 프로필 정보를 수정합니다.
	 * 수정 시 권한(본인 여부) 검증과 핸들 중복 검사를 수행합니다.
	 * 
	 * @param id 수정하려는 대상 사용자의 식별자(UUID)
	 * @param authenticatedEmail SecurityContext에서 가져온 현재 인증된 사용자의 이메일
	 * @param request 변경할 프로필 데이터(닉네임, 핸들, 이미지, 소개 등)
	 * @throws SecurityException 본인의 프로필이 아닐 경우 예외 발생
	 * @throws IllegalArgumentException 변경하려는 핸들이 이미 타인에 의해 사용 중인 경우 예외 발생
	 */
	@Override
	public void updateProfile(String id, String authenticatedEmail, UserDto.UpdateProfileRequest request) {
		// 1. 권한 검증: 토큰의 이메일로 조회한 유저의 ID가 수정하려는 대상 ID와 일치하는지 확인합니다.
		UserDto.UserEntity authUser = userMapper.findByEmail(authenticatedEmail);
		if (authUser == null || !authUser.getId().equals(id)) {
			throw new SecurityException("자신의 프로필만 수정할 수 있습니다.");
		}

		// 2. 핸들 중복 체크: 사용자가 핸들을 변경하려고 할 때, 해당 핸들이 이미 존재하는지 확인합니다.
		// (단, 기존에 본인이 사용 중인 핸들을 그대로 유지하는 경우는 허용합니다.)
		UserDto.UserEntity existingHandleUser = userMapper.findByHandle(request.getHandle());
		if (existingHandleUser != null && !existingHandleUser.getId().equals(id)) {
			throw new IllegalArgumentException("Already Exists Handle");
		}
		
		// 3. 모든 검증을 통과하면 DB에 프로필 업데이트 쿼리를 실행합니다.
		userMapper.updateProfile(id, request);
	}
}
