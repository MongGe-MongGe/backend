package com.ssafy.gourming.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "password-reset.mail")
public class PasswordResetMailProperties {

	private boolean enabled;
	private String from;

	public void validateIfEnabled() {
		if (enabled && (from == null || from.isBlank())) {
			throw new IllegalStateException("password-reset.mail.from must be set when mail is enabled");
		}
	}
}
