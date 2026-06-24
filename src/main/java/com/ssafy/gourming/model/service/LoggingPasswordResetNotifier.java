package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

	@Value("${password-reset.frontend-base-url:http://localhost:3000/reset-password}")
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
