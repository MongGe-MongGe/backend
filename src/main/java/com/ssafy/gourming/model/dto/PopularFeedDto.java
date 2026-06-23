package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PopularFeedDto {

	// 인기피드 점수 집계 과정에서 사용하는 내부 행 DTO
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PopularReviewScoreRow {

		private String reviewId;
		private double score;
		private long likeCount;
		private long commentCount;
		private int rankNo;
		private int windowDays;
		private LocalDateTime calculatedAt;
	}
}
