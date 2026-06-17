package com.ssafy.gourming.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.mapper.UserMapper;
import com.ssafy.gourming.model.service.FollowService;
import com.ssafy.gourming.model.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final FollowService followService;
	private final UserMapper userMapper;

	/**
	 * 핸들 존재 여부를 확인하는 엔드포인트입니다.
	 * 프론트엔드에서 디바운싱을 통해 입력 도중 호출되며,
	 * 이미 존재하는 핸들일 경우 true, 사용 가능한 핸들일 경우 false를 반환합니다.
	 * 
	 * @param handle 중복을 검사할 사용자 핸들(예: @testUser)
	 * @return 존재하면 true, 존재하지 않으면 false
	 */
	@GetMapping("/check-handle")
	public ResponseEntity<Boolean> checkHandle(@RequestParam String handle) {
		boolean exists = !userService.isHandleAvailable(handle);
		return ResponseEntity.ok(exists);
	}

	@GetMapping
	public ResponseEntity<?> searchUsers(
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "") String keyword,
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "20") int limit,
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "0") int offset) {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		String authenticatedEmail = (auth != null && auth.getPrincipal() instanceof String) ? (String) auth.getPrincipal() : null;
		
		java.util.List<UserDto.UserProfileResponse> users = userService.searchUsers(keyword, authenticatedEmail, limit, offset);
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{handle}")
	public ResponseEntity<?> getUserProfile(@PathVariable String handle) {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		String authenticatedEmail = (auth != null && auth.getPrincipal() instanceof String) ? (String) auth.getPrincipal() : null;
		
		UserDto.UserProfileResponse profile = userService.getUserProfile(handle, authenticatedEmail);
		return ResponseEntity.ok(profile);
	}

	@PostMapping("/follow/{userId}")
	public ResponseEntity<?> followUser(@PathVariable String userId) {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof String) || auth.getPrincipal().equals("anonymousUser")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		String authenticatedEmail = (String) auth.getPrincipal();
		UserDto.UserEntity authUser = userMapper.findByEmail(authenticatedEmail);
		if(authUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		followService.followUser(authUser.getId(), userId);
		return ResponseEntity.ok().build();
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/follow/{userId}")
	public ResponseEntity<?> unfollowUser(@PathVariable String userId) {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof String) || auth.getPrincipal().equals("anonymousUser")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		String authenticatedEmail = (String) auth.getPrincipal();
		UserDto.UserEntity authUser = userMapper.findByEmail(authenticatedEmail);
		if(authUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		followService.unfollowUser(authUser.getId(), userId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{userId}/followers")
	public ResponseEntity<?> getFollowers(@PathVariable String userId) {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		String authenticatedEmail = (auth != null && auth.getPrincipal() instanceof String) ? (String) auth.getPrincipal() : null;
		String currentUserId = null;
		if (authenticatedEmail != null && !authenticatedEmail.equals("anonymousUser")) {
			UserDto.UserEntity authUser = userMapper.findByEmail(authenticatedEmail);
			if (authUser != null) currentUserId = authUser.getId();
		}

		java.util.List<UserDto.UserProfileResponse> followers = followService.getFollowers(userId, currentUserId);
		return ResponseEntity.ok(followers);
	}

	@GetMapping("/{userId}/followings")
	public ResponseEntity<?> getFollowings(@PathVariable String userId) {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		String authenticatedEmail = (auth != null && auth.getPrincipal() instanceof String) ? (String) auth.getPrincipal() : null;
		String currentUserId = null;
		if (authenticatedEmail != null && !authenticatedEmail.equals("anonymousUser")) {
			UserDto.UserEntity authUser = userMapper.findByEmail(authenticatedEmail);
			if (authUser != null) currentUserId = authUser.getId();
		}

		java.util.List<UserDto.UserProfileResponse> followings = followService.getFollowings(userId, currentUserId);
		return ResponseEntity.ok(followings);
	}

	/**
	 * 회원의 프로필 정보(닉네임, 핸들, 프로필 이미지, 자기소개 등)를 수정합니다.
	 * 보안을 위해 URL 경로의 id와 토큰에 저장된 유저의 id가 일치하는지 검증합니다.
	 * 
	 * @param id 수정 대상 유저의 고유 식별자(UUID 등)
	 * @param authenticatedUserId JWT에서 추출한 현재 인증 사용자의 ID
	 * @param request 변경할 프로필 정보가 담긴 요청 객체
	 * @return 성공 시 200 OK
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> updateProfile(
			@PathVariable String id,
			@AuthenticationPrincipal String authenticatedUserId,
			@Valid @RequestBody UserDto.UpdateProfileRequest request
	) {
		userService.updateProfile(id, authenticatedUserId, request);
		return ResponseEntity.ok().build();
	}
}
