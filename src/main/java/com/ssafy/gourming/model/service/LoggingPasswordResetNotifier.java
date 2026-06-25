package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
// 실제 메일 발송이 꺼진 기본/개발 환경에서는 reset link를 로그로 남긴다.
@ConditionalOnProperty(
		name = "password-reset.mail.enabled",
		havingValue = "false",
		matchIfMissing = true
)
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

	@Value("${password-reset.frontend-base-url:http://localhost:5173/reset-password}")
	private String frontendBaseUrl;

	@Override
	public void notifyPasswordReset(String email, String rawToken, LocalDateTime expiresAt) {
		String resetLink = UriComponentsBuilder
				.fromUriString(frontendBaseUrl)
				.queryParam("token", rawToken)
				.build()
				.toUriString();

		log.info("Password reset link for {}: {} (expiresAt={})", email, resetLink, expiresAt);
	}
}
