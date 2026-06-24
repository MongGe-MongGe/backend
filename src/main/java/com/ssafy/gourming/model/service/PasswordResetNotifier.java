package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;

public interface PasswordResetNotifier {

	void notifyPasswordReset(String email, String rawToken, LocalDateTime expiresAt);
}
