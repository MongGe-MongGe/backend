package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;

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

	// 추천 피드 응답. 다음 페이지 요청에 그대로 보낼 기준 시각(asOf)을 함께 담는다.
	// ReviewPageResponse를 상속해 다른 피드 응답에는 asOf 필드가 생기지 않게 한다.
	@Getter
	@Setter
	@NoArgsConstructor
	public static class RecommendationPageResponse extends ReviewPageResponse {

		private LocalDateTime asOf;

		public static RecommendationPageResponse of(ReviewPageResponse page, LocalDateTime asOf) {
			RecommendationPageResponse response = new RecommendationPageResponse();
			response.setContent(page.getContent());
			response.setPage(page.getPage());
			response.setSize(page.getSize());
			response.setTotalElements(page.getTotalElements());
			response.setTotalPages(page.getTotalPages());
			response.setFirst(page.isFirst());
			response.setLast(page.isLast());
			response.setAsOf(asOf);
			return response;
		}
	}
}
