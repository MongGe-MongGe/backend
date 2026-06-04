package com.ssafy.gourming.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public class UserDto {
	
	@Getter
	@NoArgsConstructor
	public static class SignupRequest {
		private String email;
		private String nickname;
		@Setter
		private String password; // 비밀번호 암호화를 위해 이 필드에만 setter 적용
		private String phone;
		private String handle;
	}
	
	@Getter
	@NoArgsConstructor
	public static class LoginRequest {
		private String email;
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
	
	@Getter
	@AllArgsConstructor
	public static class UserProfileResponse {
		private String id;
		private String nickname;
		private String handle;
		private String profileImage;
		private String bio;
	}
}
