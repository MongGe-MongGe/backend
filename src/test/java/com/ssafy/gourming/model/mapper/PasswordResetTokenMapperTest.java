package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.gourming.model.dto.PasswordResetDto;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("PasswordResetTokenMapper test")
class PasswordResetTokenMapperTest {

	private static final String USER_ID = "97000000-0000-0000-0000-000000000001";

	@Autowired
	private PasswordResetTokenMapper passwordResetTokenMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertTestUser();
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("stores a token and finds it when it is unused and unexpired")
	void insertTokenAndFindValidTokenByHash() {
		// 유효 토큰의 기준은 tokenHash 일치, 미사용, 만료 전이다.
		LocalDateTime now = LocalDateTime.now();
		PasswordResetDto.PasswordResetTokenEntity token =
				createToken("valid-token-hash", now.plusMinutes(30), null);

		int insertedCount = passwordResetTokenMapper.insertToken(token);
		PasswordResetDto.PasswordResetTokenEntity selected =
				passwordResetTokenMapper.findValidTokenByHash("valid-token-hash", now);

		assertThat(insertedCount).isEqualTo(1);
		assertThat(selected).isNotNull();
		assertThat(selected.getId()).isEqualTo(token.getId());
		assertThat(selected.getUserId()).isEqualTo(USER_ID);
		assertThat(selected.getTokenHash()).isEqualTo("valid-token-hash");
		assertThat(selected.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("does not find expired tokens as valid")
	void findValidTokenByHashExcludesExpiredToken() {
		LocalDateTime now = LocalDateTime.now();
		PasswordResetDto.PasswordResetTokenEntity token =
				createToken("expired-token-hash", now.minusMinutes(1), null);

		passwordResetTokenMapper.insertToken(token);
		PasswordResetDto.PasswordResetTokenEntity selected =
				passwordResetTokenMapper.findValidTokenByHash("expired-token-hash", now);

		assertThat(selected).isNull();
	}

	@Test
	@DisplayName("does not find used tokens as valid")
	void findValidTokenByHashExcludesUsedToken() {
		LocalDateTime now = LocalDateTime.now();
		PasswordResetDto.PasswordResetTokenEntity token =
				createToken("used-token-hash", now.plusMinutes(30), now.minusMinutes(1));

		passwordResetTokenMapper.insertToken(token);
		PasswordResetDto.PasswordResetTokenEntity selected =
				passwordResetTokenMapper.findValidTokenByHash("used-token-hash", now);

		assertThat(selected).isNull();
	}

	@Test
	@DisplayName("marks an unused token as used")
	void markTokenUsed() {
		LocalDateTime now = LocalDateTime.now();
		PasswordResetDto.PasswordResetTokenEntity token =
				createToken("mark-used-token-hash", now.plusMinutes(30), null);

		passwordResetTokenMapper.insertToken(token);
		int updatedCount = passwordResetTokenMapper.markTokenUsed(token.getId(), now);
		PasswordResetDto.PasswordResetTokenEntity selected =
				passwordResetTokenMapper.findValidTokenByHash("mark-used-token-hash", now.minusSeconds(1));

		assertThat(updatedCount).isEqualTo(1);
		assertThat(selected).isNull();
	}

	@Test
	@DisplayName("deletes only unused tokens for a user")
	void deleteUnusedTokensByUserId() {
		// 재요청 시 기존 미사용 토큰만 제거하고 이미 사용된 이력은 남긴다.
		LocalDateTime now = LocalDateTime.now();
		PasswordResetDto.PasswordResetTokenEntity firstUnused =
				createToken("unused-token-hash-1", now.plusMinutes(30), null);
		PasswordResetDto.PasswordResetTokenEntity secondUnused =
				createToken("unused-token-hash-2", now.plusMinutes(30), null);
		PasswordResetDto.PasswordResetTokenEntity used =
				createToken("already-used-token-hash", now.plusMinutes(30), now.minusMinutes(1));

		passwordResetTokenMapper.insertToken(firstUnused);
		passwordResetTokenMapper.insertToken(secondUnused);
		passwordResetTokenMapper.insertToken(used);

		int deletedCount = passwordResetTokenMapper.deleteUnusedTokensByUserId(USER_ID);

		assertThat(deletedCount).isEqualTo(2);
		assertThat(countTokenByHash("unused-token-hash-1")).isZero();
		assertThat(countTokenByHash("unused-token-hash-2")).isZero();
		assertThat(countTokenByHash("already-used-token-hash")).isEqualTo(1);
	}

	@Test
	@DisplayName("deletes only expired tokens")
	void deleteExpiredTokens() {
		LocalDateTime now = LocalDateTime.now();
		PasswordResetDto.PasswordResetTokenEntity expired =
				createToken("expired-delete-token-hash", now.minusMinutes(1), null);
		PasswordResetDto.PasswordResetTokenEntity valid =
				createToken("valid-keep-token-hash", now.plusMinutes(30), null);

		passwordResetTokenMapper.insertToken(expired);
		passwordResetTokenMapper.insertToken(valid);

		int deletedCount = passwordResetTokenMapper.deleteExpiredTokens(now);

		assertThat(deletedCount).isGreaterThanOrEqualTo(1);
		assertThat(countTokenByHash("expired-delete-token-hash")).isZero();
		assertThat(countTokenByHash("valid-keep-token-hash")).isEqualTo(1);
	}

	private PasswordResetDto.PasswordResetTokenEntity createToken(
			String tokenHash,
			LocalDateTime expiresAt,
			LocalDateTime usedAt
	) {
		return new PasswordResetDto.PasswordResetTokenEntity(
				UUID.randomUUID().toString(),
				USER_ID,
				tokenHash,
				expiresAt,
				usedAt,
				null
		);
	}

	private void insertTestUser() {
		jdbcTemplate.update(
				"""
				INSERT INTO users (id, email, password, handle, nickname)
				VALUES (?, ?, ?, ?, ?)
				""",
				USER_ID,
				"password-reset-token@test.com",
				"test-password",
				"@password_reset_token",
				"password reset token test user"
		);
	}

	private int countTokenByHash(String tokenHash) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = ?",
				Integer.class,
				tokenHash
		);
		return count == null ? 0 : count;
	}

	private void deleteTestData() {
		jdbcTemplate.update(
				"DELETE FROM password_reset_tokens WHERE user_id = ?",
				USER_ID
		);
		jdbcTemplate.update(
				"DELETE FROM users WHERE id = ?",
				USER_ID
		);
	}
}
