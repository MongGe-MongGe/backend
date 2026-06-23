package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.gourming.model.dto.TasteTagDto.TasteTag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ReviewAnalysisDto {

	// ReviewTagAnalyzer 입력값
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewTagAnalysisRequest {

		private String reviewId;
		private String content;
		private List<TasteTag> allowedTags = new ArrayList<>();
	}

	// ReviewTagAnalyzer 출력값
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewTagAnalysisResult {

		private String reviewId;
		private String extractorVersion;
		private List<ReviewTagScore> tags = new ArrayList<>();
	}

	// 리뷰 한 건에서 추출된 태그 점수
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewTagScore {

		private String code;
		private Double relevance;
		private Double sentiment;
	}

	// review_analysis_states 테이블과 매핑되는 분석 상태
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewAnalysisState {

		private String reviewId;
		private AnalysisStatus status;
		private String contentHash;
		private String extractorVersion;
		private int retryCount;
		private LocalDateTime nextRetryAt;
		private String errorCode;
		private String errorMessage;
		private LocalDateTime analyzedAt;
		private LocalDateTime updatedAt;
	}

	// review_taste_tags batch insert에 사용하는 내부 row
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewTagRow {

		private String reviewId;
		private Long tagId;
		private Double relevance;
		private Double sentiment;
		private String source;
		private String extractorVersion;
	}

	// REVIEW 대상 batch 처리 결과. 2차 PLACE 처리에서 successPlaceIds를 이어받는다.
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewAnalysisBatchResult {

		private List<String> successReviewIds = new ArrayList<>();
		private List<String> successPlaceIds = new ArrayList<>();
		private int completedCount;
		private int failedCount;
		private int skippedCount;
		private int deadCount;
	}
}
