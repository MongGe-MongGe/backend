package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PasswordResetDto {

	// DB에는 이메일로 발송한 토큰 원문을 저장하지 않고 해시값만 저장한다.
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PasswordResetTokenEntity {
		private String id;
		private String userId;
		private String tokenHash;
		private LocalDateTime expiresAt;
		private LocalDateTime usedAt;
		private LocalDateTime createdAt;
	}

	@Getter
	@NoArgsConstructor
	public static class PasswordResetRequest {
		@NotBlank(message = "이메일은 필수 입력값입니다")
		@Email(message = "올바른 이메일 형식이 아닙니다")
		private String email;
	}
}
