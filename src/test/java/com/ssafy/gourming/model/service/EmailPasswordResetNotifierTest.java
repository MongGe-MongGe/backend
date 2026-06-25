package com.ssafy.gourming.model.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.ssafy.gourming.config.PasswordResetMailProperties;

@ExtendWith(MockitoExtension.class)
class EmailPasswordResetNotifierTest {

	@Mock
	private JavaMailSender mailSender;

	@Test
	@DisplayName("비밀번호 재설정 메일을 발송한다")
	void notifyPasswordReset_sendsMail() {
		EmailPasswordResetNotifier notifier = createNotifier("no-reply@gourming.test");
		LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 25, 15, 30);

		notifier.notifyPasswordReset("user@test.com", "raw reset/token", expiresAt);

		ArgumentCaptor<SimpleMailMessage> messageCaptor =
				ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());

		SimpleMailMessage message = messageCaptor.getValue();
		assertEquals("user@test.com", message.getTo()[0]);
		assertEquals("no-reply@gourming.test", message.getFrom());
		assertEquals("[Gourming] 비밀번호 재설정 안내", message.getSubject());
		assertTrue(message.getText().contains("http://localhost:5173/reset-password?token="));
		assertTrue(message.getText().contains("raw reset/token"));
		assertTrue(message.getText().contains("2026-06-25 15:30 KST"));
	}

	@Test
	@DisplayName("메일 발송 실패는 예외로 전파하지 않는다")
	void notifyPasswordReset_mailSendFailure_doesNotThrow() {
		EmailPasswordResetNotifier notifier = createNotifier("no-reply@gourming.test");
		doThrow(new MailSendException("SMTP failed"))
				.when(mailSender)
				.send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

		assertDoesNotThrow(() ->
				notifier.notifyPasswordReset(
						"user@test.com",
						"raw-token",
						LocalDateTime.of(2026, 6, 25, 15, 30)
				)
		);
	}

	@Test
	@DisplayName("메일 활성화 상태에서 발신자 설정이 없으면 실패한다")
	void constructor_enabledWithoutFrom_throwsException() {
		PasswordResetMailProperties properties = new PasswordResetMailProperties();
		properties.setEnabled(true);
		properties.setFrom(" ");

		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> new EmailPasswordResetNotifier(
						mailSender,
						properties,
						"http://localhost:5173/reset-password"
				)
		);

		assertEquals("password-reset.mail.from must be set when mail is enabled", exception.getMessage());
	}

	private EmailPasswordResetNotifier createNotifier(String from) {
		PasswordResetMailProperties properties = new PasswordResetMailProperties();
		properties.setEnabled(true);
		properties.setFrom(from);

		return new EmailPasswordResetNotifier(
				mailSender,
				properties,
				"http://localhost:5173/reset-password"
		);
	}
}
