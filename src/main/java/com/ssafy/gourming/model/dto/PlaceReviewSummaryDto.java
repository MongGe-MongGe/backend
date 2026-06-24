package com.ssafy.gourming.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PlaceReviewSummaryDto {

	// place_review_summaries 테이블과 매핑되는 내부 엔티티
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceReviewSummaryEntity {

		private String placeId;
		private String summary;
		private String positivePointsJson;
		private String negativePointsJson;
		private String recommendedForJson;
		private String keywordsJson;
		private int reviewCount;
		private String modelVersion;
		private String status;
		private LocalDateTime lastReviewUpdatedAt;
		private String errorMessage;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}

	// 장소 상세 응답에 포함되는 리뷰 요약 정보
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceReviewSummaryResponse {

		private String placeId;
		private String summary;
		private List<String> positivePoints;
		private List<String> negativePoints;
		private List<String> recommendedFor;
		private List<String> keywords;
		private int reviewCount;
		private String status;
		private LocalDateTime lastReviewUpdatedAt;
		private LocalDateTime updatedAt;
	}

	// AI 요약 생성 결과
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceReviewSummaryGenerateResult {

		private String summary;
		private List<String> positivePoints;
		private List<String> negativePoints;
		private List<String> recommendedFor;
		private List<String> keywords;
		private int reviewCount;
		private LocalDateTime lastReviewUpdatedAt;
	}

	// 전체 장소 요약 갱신 결과
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceReviewSummaryBulkRefreshResponse {

		private int requestedCount;
		private int successCount;
		private int failedCount;
		private List<String> failedPlaceIds;
		private LocalDateTime startedAt;
		private LocalDateTime finishedAt;
	}

	// AI 요약 원본으로 사용하는 리뷰 row
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewSummarySourceRow {

		private String reviewId;
		private String content;
		private Integer ratingScore;
		private LocalDate visitedAt;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}
}
