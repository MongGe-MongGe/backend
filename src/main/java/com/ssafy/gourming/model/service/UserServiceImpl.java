package com.ssafy.gourming.model.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.NoSuchElementException;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.dao.UserMapper;
import com.ssafy.gourming.model.dto.UserDto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	
	@Value("${jwt.secret}")
	private String jwtSecret;
	
	@Value("${jwt.expiration-ms}")
	private long jwtExpirationMs;
	
	@Override
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
	}

	@Override
	public UserDto.LoginResponse login(UserDto.LoginRequest request) {
		// 1. 이메일로 사용자 조회
		UserDto.UserEntity user = userMapper.findByEmail(request.getEmail());
		
		// 2. 사용자가 없거나 비밀번호 불일치 -> 동일 메시지로 예외처리
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Invalid Email or Password");
		}
		
		// 3. JWT 생성 후 응답 반환 (프로필 정보 전체를 Body에 포함)
		String token = generateToken(user.getEmail());
		return new UserDto.LoginResponse(
				token, 
				user.getId(), 
				user.getNickname(),
				user.getEmail(),
				user.getHandle(), 
				user.getProfileImage()
			);
	}
	
	
	private String generateToken(String email) {
		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		return Jwts.builder()
				.subject(email)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
				.signWith(key)
				.compact();
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
}
