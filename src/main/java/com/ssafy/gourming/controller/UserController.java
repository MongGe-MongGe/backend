package com.ssafy.gourming.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.service.FollowService;
import com.ssafy.gourming.model.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final FollowService followService;

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

	/**
	 * 이메일 존재 여부를 확인하는 엔드포인트입니다.
	 * 프론트엔드에서 디바운싱을 통해 입력 도중 호출되며,
	 * 이미 존재하는 이메일일 경우 true, 사용 가능한 이메일일 경우 false를 반환합니다.
	 * 
	 * @param email 중복을 검사할 사용자 이메일
	 * @return 존재하면 true, 존재하지 않으면 false
	 */
	@GetMapping("/check-email")
	public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
		boolean exists = !userService.isEmailAvailable(email);
		return ResponseEntity.ok(exists);
	}

	/**
	 * 유저 검색 API (페이지네이션 지원)
	 * 닉네임이나 핸들에 검색어가 포함된 사용자 목록을 반환합니다.
	 * 인증된 사용자인 경우, 검색된 각 사용자와의 팔로우 여부(isFollowing) 및 맞팔로우 여부(isFollower)를 함께 반환합니다.
	 * 
	 * @param keyword 검색할 키워드 (기본값: 빈 문자열)
	 * @param limit   반환할 최대 항목 수 (기본값: 20)
	 * @param offset  결과 시작 위치 (기본값: 0)
	 * @return 검색된 사용자 프로필 응답 목록
	 */
	@GetMapping
	public ResponseEntity<?> searchUsers(
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "") String keyword,
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "20") int limit,
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "0") int offset,
			@AuthenticationPrincipal String currentUserId) {
		java.util.List<UserDto.UserProfileResponse> users = userService.searchUsers(keyword, currentUserId, limit, offset);
		return ResponseEntity.ok(users);
	}

	/**
	 * 특정 사용자의 프로필 상세 정보 조회 API
	 * 핸들을 통해 대상 사용자의 기본 정보 및 팔로우 통계(팔로워, 팔로잉 수)를 조회합니다.
	 * 인증된 사용자인 경우, 나와 대상 사용자의 팔로우 상태를 포함하여 반환합니다.
	 * 
	 * @param handle 조회할 대상 사용자의 고유 핸들
	 * @return 대상 사용자의 프로필 응답 객체
	 */
	@GetMapping("/{handle}")
	public ResponseEntity<?> getUserProfile(@PathVariable String handle, @AuthenticationPrincipal String currentUserId) {
		UserDto.UserProfileResponse profile = userService.getUserProfile(handle, currentUserId);
		return ResponseEntity.ok(profile);
	}

	/**
	 * 타 사용자 팔로우 API (인증 필수)
	 * 현재 로그인한 사용자가 대상 사용자를 팔로우합니다.
	 * 
	 * @param userId 팔로우할 대상 사용자의 식별자(UUID)
	 * @return HTTP 200 (성공) 또는 401 (비인증 상태)
	 */
	@PostMapping("/follow/{userId}")
	public ResponseEntity<?> followUser(@PathVariable String userId, @AuthenticationPrincipal String currentUserId) {
		if (currentUserId == null || currentUserId.equals("anonymousUser")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		followService.followUser(currentUserId, userId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 타 사용자 팔로우 취소(언팔로우) API (인증 필수)
	 * 현재 로그인한 사용자가 대상 사용자에 대한 팔로우를 취소합니다.
	 * 
	 * @param userId 언팔로우할 대상 사용자의 식별자(UUID)
	 * @return HTTP 200 (성공) 또는 401 (비인증 상태)
	 */
	@org.springframework.web.bind.annotation.DeleteMapping("/follow/{userId}")
	public ResponseEntity<?> unfollowUser(@PathVariable String userId, @AuthenticationPrincipal String currentUserId) {
		if (currentUserId == null || currentUserId.equals("anonymousUser")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		followService.unfollowUser(currentUserId, userId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 특정 사용자를 팔로우하는 팔로워 목록 조회 API
	 * 비인증 사용자도 조회 가능하며, 인증된 사용자일 경우 목록 내 유저들과의 상호 팔로우 상태가 추가 도출됩니다.
	 * 
	 * @param userId 조회 대상 사용자의 식별자(UUID)
	 * @return 팔로워 사용자들의 프로필 응답 목록
	 */
	@GetMapping("/{userId}/followers")
	public ResponseEntity<?> getFollowers(@PathVariable String userId, @AuthenticationPrincipal String currentUserId) {
		java.util.List<UserDto.UserProfileResponse> followers = followService.getFollowers(userId, currentUserId);
		return ResponseEntity.ok(followers);
	}

	/**
	 * 특정 사용자가 팔로우하는 팔로잉 목록 조회 API
	 * 비인증 사용자도 조회 가능하며, 인증된 사용자일 경우 목록 내 유저들과의 상호 팔로우 상태가 추가 도출됩니다.
	 * 
	 * @param userId 조회 대상 사용자의 식별자(UUID)
	 * @return 팔로잉 사용자들의 프로필 응답 목록
	 */
	@GetMapping("/{userId}/followings")
	public ResponseEntity<?> getFollowings(@PathVariable String userId, @AuthenticationPrincipal String currentUserId) {
		java.util.List<UserDto.UserProfileResponse> followings = followService.getFollowings(userId, currentUserId);
		return ResponseEntity.ok(followings);
	}

	/**
	 * 회원의 프로필 정보(닉네임, 핸들, 프로필 이미지, 자기소개 등)를 수정합니다.
	 * 보안을 위해 URL 경로의 id와 토큰에 저장된 유저의 id가 일치하는지 검증합니다.
	 * 
	 * @param id                  수정 대상 유저의 고유 식별자(UUID 등)
	 * @param authenticatedUserId JWT에서 추출한 현재 인증 사용자의 ID
	 * @param request             변경할 프로필 정보가 담긴 요청 객체
	 * @return 성공 시 200 OK
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> updateProfile(
			@PathVariable String id,
			@AuthenticationPrincipal String authenticatedUserId,
			@Valid @RequestBody UserDto.UpdateProfileRequest request) {
		userService.updateProfile(id, authenticatedUserId, request);
		return ResponseEntity.ok().build();
	}

	/**
	 * 사용자 역할(role) 변경 API (인증 필수)
	 * 본인만 자신의 역할을 USER ↔ ADMIN으로 변경할 수 있습니다.
	 *
	 * @param id                  변경 대상 유저의 식별자(UUID)
	 * @param authenticatedUserId JWT에서 추출한 현재 인증 사용자의 ID
	 * @param role                변경할 역할 값 ("USER" 또는 "ADMIN")
	 * @return 성공 시 200 OK
	 */
	@PatchMapping("/{id}/role")
	public ResponseEntity<?> updateRole(
			@PathVariable String id,
			@AuthenticationPrincipal String authenticatedUserId,
			@RequestParam String role) {
		if (authenticatedUserId == null || authenticatedUserId.equals("anonymousUser")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		userService.updateRole(id, authenticatedUserId, role);
		return ResponseEntity.ok().build();
	}
}
