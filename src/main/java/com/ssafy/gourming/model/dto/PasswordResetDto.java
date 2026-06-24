package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

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
}
