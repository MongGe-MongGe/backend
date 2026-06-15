package com.ssafy.gourming.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/**
	 * 핸들 중복 여부를 확인하는 엔드포인트입니다.
	 * 프론트엔드에서 디바운싱을 통해 입력 도중 호출되며,
	 * 사용 가능한 핸들일 경우 true, 이미 존재하는 핸들일 경우 false를 반환합니다.
	 * 
	 * @param handle 중복을 검사할 사용자 핸들(예: @testUser)
	 * @return { "available": boolean } 형태의 JSON 객체
	 */
	@GetMapping("/check-handle")
	public ResponseEntity<?> checkHandle(@RequestParam String handle) {
		if (!handle.matches(com.ssafy.gourming.util.ValidationConstants.HANDLE_REGEX)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(com.ssafy.gourming.util.ValidationConstants.HANDLE_MESSAGE);
		}
		boolean isAvailable = userService.isHandleAvailable(handle);
		return ResponseEntity.ok(Map.of("available", isAvailable));
	}

	@GetMapping("/{handle}")
	public ResponseEntity<?> getUserProfile(@PathVariable String handle) {
		UserDto.UserProfileResponse profile = userService.getUserProfile(handle);
		return ResponseEntity.ok(profile);
	}

	/**
	 * 회원의 프로필 정보(닉네임, 핸들, 프로필 이미지, 자기소개 등)를 수정합니다.
	 * 보안을 위해 URL 경로의 id와 토큰에 저장된 유저의 id가 일치하는지 검증합니다.
	 * 
	 * @param id 수정 대상 유저의 고유 식별자(UUID 등)
	 * @param request 변경할 프로필 정보가 담긴 요청 객체
	 * @return 성공 시 200 OK
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> updateProfile(@PathVariable String id, @Valid @RequestBody UserDto.UpdateProfileRequest request) {
		// 1. SecurityContext에서 JwtFilter를 통해 등록된 현재 인증된 사용자의 식별자(이메일)를 가져옵니다.
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String authenticatedEmail = (String) auth.getPrincipal();

		// 2. 서비스 레이어에 대상 id, 인증된 이메일, 수정 요청 데이터를 전달하여 비즈니스 로직(검증 및 업데이트)을 수행합니다.
		userService.updateProfile(id, authenticatedEmail, request);
		return ResponseEntity.ok().build();
	}
}
