package com.ssafy.gourming.model.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.PasswordResetDto;

@Mapper
public interface PasswordResetTokenMapper {

	// 새 비밀번호 재설정 토큰 해시를 저장한다.
	int insertToken(PasswordResetDto.PasswordResetTokenEntity token);

	// 아직 사용되지 않았고 만료되지 않은 토큰만 유효 토큰으로 조회한다.
	PasswordResetDto.PasswordResetTokenEntity findValidTokenByHash(
			@Param("tokenHash") String tokenHash,
			@Param("now") LocalDateTime now
	);

	// 토큰 재사용을 막기 위해 사용 완료 시각을 기록한다.
	int markTokenUsed(
			@Param("id") String id,
			@Param("usedAt") LocalDateTime usedAt
	);

	// 같은 사용자가 재요청하면 이전 미사용 토큰을 정리한다.
	int deleteUnusedTokensByUserId(@Param("userId") String userId);

	// 이후 스케줄러에서 만료 토큰을 주기적으로 정리할 때 사용한다.
	int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
