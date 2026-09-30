package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class RecommendationDto {

	// 추천 후보 리뷰. API 응답으로 노출하지 않는다.
	@Getter
	@Setter
	@NoArgsConstructor
	public static class RecommendationCandidateRow {

		private String reviewId;
		private String placeId;
		private float[] embedding;
		private LocalDateTime createdAt;
	}
}
