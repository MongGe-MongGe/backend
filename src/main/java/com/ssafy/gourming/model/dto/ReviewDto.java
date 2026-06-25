package com.ssafy.gourming.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ReviewDto {

	// reviews 테이블과 매핑되는 내부 엔티티
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewEntity {

		private String id;
		private String content;
		private List<String> images;
		private Integer ratingScore;
		private LocalDate visitedAt;
		private String placeId;
		private String userId;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}

	// 리뷰 생성 요청
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewCreateRequest {

		@Valid
		@NotNull(message = "Place information is required.")
		private PlaceRequest place;

		@NotBlank(message = "Review content is required.")
		private String content;

		private List<String> images;

		@Min(value = 1, message = "Rating score must be between 1 and 5.")
		@Max(value = 5, message = "Rating score must be between 1 and 5.")
		private Integer ratingScore;

		private LocalDate visitedAt;
	}

	// 리뷰 수정 요청
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewUpdateRequest {

		@NotBlank(message = "Review content is required.")
		private String content;

		private List<String> images;

		@Min(value = 1, message = "Rating score must be between 1 and 5.")
		@Max(value = 5, message = "Rating score must be between 1 and 5.")
		private Integer ratingScore;

		private LocalDate visitedAt;
	}

	// 리뷰 단건 및 목록 조회 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewResponse {

		private String id;
		private String content;
		private List<String> images;
		private Integer ratingScore;
		private LocalDate visitedAt;
		private PlaceSummary place;
		private AuthorSummary author;
		private long likeCount;
		private long commentCount;
		private boolean likedByMe;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}

	// 페이지네이션이 적용된 리뷰 목록 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewPageResponse {

		private List<ReviewResponse> content;
		private int page;
		private int size;
		private long totalElements;
		private int totalPages;
		private boolean first;
		private boolean last;
	}

	// 리뷰 응답에 포함되는 장소 요약 정보
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceSummary {

		private String id;
		private String name;
		private String categoryName;
		private String categoryGroupCode;
		private String roadAddressName;
		private String x;
		private String y;
		
		@com.fasterxml.jackson.annotation.JsonProperty("isSaved")
		private boolean isSaved;
		
		private com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryResponse reviewSummary;
	}

	// 리뷰 응답에 포함되는 작성자 요약 정보
	@Getter
	@Setter
	@NoArgsConstructor
	public static class AuthorSummary {

		private String id;
		private String nickname;
		private String handle;
		private String profileImage;
		
		@com.fasterxml.jackson.annotation.JsonProperty("isFollowing")
		private boolean isFollowing;
	}
}
