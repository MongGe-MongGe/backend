package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.ssafy.gourming.config.PasswordResetMailProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
// 실제 메일 발송이 켜진 환경에서만 PasswordResetNotifier 구현체로 등록된다.
@ConditionalOnProperty(
		name = "password-reset.mail.enabled",
		havingValue = "true"
)
public class EmailPasswordResetNotifier implements PasswordResetNotifier {

	private static final String SUBJECT = "[Gourming] 비밀번호 재설정 안내";
	private static final DateTimeFormatter EXPIRES_AT_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final JavaMailSender mailSender;
	private final PasswordResetMailProperties mailProperties;
	private final String frontendBaseUrl;

	public EmailPasswordResetNotifier(
			JavaMailSender mailSender,
			PasswordResetMailProperties mailProperties,
			@Value("${password-reset.frontend-base-url:http://localhost:5173/reset-password}") String frontendBaseUrl
	) {
		this.mailSender = mailSender;
		this.mailProperties = mailProperties;
		this.frontendBaseUrl = frontendBaseUrl;
		// SMTP 호출 전에 필수 발신자 설정 누락을 애플리케이션 시작 시점에 드러낸다.
		this.mailProperties.validateIfEnabled();
	}

	@Override
	public void notifyPasswordReset(String email, String rawToken, LocalDateTime expiresAt) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setFrom(mailProperties.getFrom().trim());
		message.setSubject(SUBJECT);
		message.setText(createBody(createResetLink(rawToken), expiresAt));

		try {
			mailSender.send(message);
		} catch (MailException exception) {
			// 가입 여부가 응답 코드 차이로 드러나지 않도록 발송 실패는 로그만 남긴다.
			log.error("Failed to send password reset mail to {}", email, exception);
		}
	}

	private String createResetLink(String rawToken) {
		// 토큰은 원문이 이메일 링크에만 담기고, DB에는 해시만 저장된다.
		return UriComponentsBuilder
				.fromUriString(frontendBaseUrl)
				.queryParam("token", rawToken)
				.build()
				.toUriString();
	}

	private String createBody(String resetLink, LocalDateTime expiresAt) {
		// LocalDateTime에는 zone 정보가 없으므로 서비스 기준 시간대인 KST를 명시한다.
		return """
				안녕하세요, Gourming입니다.
				
				비밀번호 재설정을 요청하셨습니다.
				아래 링크를 눌러 새 비밀번호를 설정해 주세요.
				
				%s
				
				이 링크는 %s KST까지 사용할 수 있습니다.
				본인이 요청하지 않았다면 이 메일을 무시해 주세요.
				""".formatted(resetLink, EXPIRES_AT_FORMATTER.format(expiresAt));
	}
}
