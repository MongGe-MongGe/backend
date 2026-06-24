package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GroupDto {

	// groups 테이블과 매핑되는 내부 엔티티
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupEntity {

		private String id;
		private String userId;
		private String name;
		private boolean defaultGroup;
		private LocalDateTime createdAt;
	}

	// 사용자 그룹 생성 요청
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupCreateRequest {

		@NotBlank(message = "그룹 이름은 필수입니다.")
		@Size(max = 100, message = "그룹 이름은 100자 이하여야 합니다.")
		private String name;
	}

	// 사용자 그룹 이름 수정 요청
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupUpdateRequest {

		@NotBlank(message = "그룹 이름은 필수입니다.")
		@Size(max = 100, message = "그룹 이름은 100자 이하여야 합니다.")
		private String name;
	}

	// 그룹 목록 및 생성·수정 결과 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupResponse {

		private String id;
		private String name;
		private boolean defaultGroup;
		private int goodPlaceCount;
		private LocalDateTime createdAt;
	}

	/**
	 * 유저의 기본 프로필 정보 DTO
	 * 그룹 소유자 등의 정보를 간략하게 전달할 때 사용됩니다.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	public static class UserSimpleInfo {
		private String id;
		private String nickname;
		private String handle;
		private String profileImage;
	}

	/**
	 * 그룹 정보와 해당 그룹을 소유한 유저 정보를 함께 반환하는 응답 DTO
	 * 팔로잉 유저들의 그룹 목록 등을 조회할 때 사용됩니다.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupWithUserResponse {
		private String id;
		private String name;
		private boolean defaultGroup;
		private int goodPlaceCount;
		private LocalDateTime createdAt;
		private UserSimpleInfo user;
	}
}
