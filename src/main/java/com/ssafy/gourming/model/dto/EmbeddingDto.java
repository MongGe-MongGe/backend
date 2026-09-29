package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class EmbeddingDto {

	// 배치가 임베딩해야 할 리뷰
	@Getter
	@Setter
	@NoArgsConstructor
	public static class EmbeddingTargetRow {

		private String reviewId;
		private String content;
	}

	// review_embeddings 테이블과 매핑되는 row
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewEmbeddingRow {

		private String reviewId;
		private float[] embedding;
		private String embedderVersion;
		private LocalDateTime updatedAt;
	}

	// place_embeddings 테이블과 매핑되는 row
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceEmbeddingRow {

		public static final String SOURCE_REVIEWS = "REVIEWS";
		public static final String SOURCE_CATEGORY = "CATEGORY";

		private String placeId;
		private float[] embedding;
		private String source;
		private int reviewCount;
		private String embedderVersion;
		private LocalDateTime updatedAt;
	}

	// 관리자 재계산 API 응답이자 배치 실행 결과
	@Getter
	@Setter
	@NoArgsConstructor
	public static class EmbeddingRefreshResponse {

		private int reviewRequestedCount;
		private int reviewSuccessCount;
		private int reviewFailedCount;
		private int placeRequestedCount;
		private int placeSuccessCount;
		private int placeFailedCount;
		private String embedderVersion;
		private LocalDateTime startedAt;
		private LocalDateTime finishedAt;
	}
}
