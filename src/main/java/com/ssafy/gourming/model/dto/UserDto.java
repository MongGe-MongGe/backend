package com.ssafy.gourming.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public class UserDto {
	
	@Getter
	@NoArgsConstructor
	public static class SignupRequest {
		@NotBlank(message = "이메일은 필수 입력값입니다")
		@Email(message = "올바른 이메일 형식이 아닙니다")
		private String email;

		@NotBlank(message = "닉네임은 필수 입력값입니다")
		@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
		private String nickname;

		@Setter
		@NotBlank(message = "비밀번호는 필수 입력값입니다")
		@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
		private String password; // 비밀번호 암호화를 위해 이 필드에만 setter 적용

		private String phone;

		@NotBlank(message = "핸들은 필수 입력값입니다")
		@Pattern(regexp = com.ssafy.gourming.util.ValidationConstants.HANDLE_REGEX,
		         message = com.ssafy.gourming.util.ValidationConstants.HANDLE_MESSAGE)
		private String handle;
	}
	
	@Getter
	@NoArgsConstructor
	public static class LoginRequest {
		@NotBlank(message = "이메일은 필수 입력값입니다")
		@Email(message = "올바른 이메일 형식이 아닙니다")
		private String email;

		@NotBlank(message = "비밀번호는 필수 입력값입니다")
		private String password;
	}
	
	@Getter
	@AllArgsConstructor
	public static class LoginResponse {
		private String Token;
		private String id;
		private String nickname;
		private String email;
		private String handle;
		private String profileImage;
	}
	
	@Getter
	@NoArgsConstructor
	public static class UserEntity {
		private String id;
		private String email;
		private String password; // bcrypt hash
		private String nickname;
		private String handle;
		private String phone;
		private String profileImage;
		private String bio;       // 자기소개 (프로필 조회에서 사용)
	}
	
	/**
	 * 프로필 조회 시 반환되는 DTO입니다.
	 * 민감한 정보(비밀번호, 연락처, 이메일)를 제외하고 안전한 정보만 포함합니다.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UserProfileResponse {
		private String id;
		private String nickname;
		private String handle;
		private String profileImage;
		private String bio;
		
		// 확장 필드
		private int followerCount;
		private int followingCount;
		
		@com.fasterxml.jackson.annotation.JsonProperty("isFollowing")
		private boolean isFollowing;

		@com.fasterxml.jackson.annotation.JsonProperty("isFollower")
		private boolean isFollower;
	}

	/**
	 * 프로필 수정 요청 시 클라이언트로부터 전달받는 데이터 DTO입니다.
	 * 닉네임, 핸들, 프로필 이미지, 자기소개 필드의 유효성 검사 규칙을 포함합니다.
	 */
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UpdateProfileRequest {
		@NotBlank(message = "닉네임은 필수 입력값입니다")
		@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
		private String nickname;

		@NotBlank(message = "핸들은 필수 입력값입니다")
		@Pattern(regexp = com.ssafy.gourming.util.ValidationConstants.HANDLE_REGEX,
		         message = com.ssafy.gourming.util.ValidationConstants.HANDLE_MESSAGE)
		private String handle;

		private String profileImage;
		private String bio;
	}
}
