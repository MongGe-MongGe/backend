package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class RecomputeTargetDto {

	// recompute_targets 테이블과 매핑되는 배치 재계산 대상
	@Getter
	@Setter
	@NoArgsConstructor
	public static class RecomputeTarget {

		private RecomputeTargetType targetType;
		private String targetId;
		private LocalDateTime requestedAt;
	}
}
