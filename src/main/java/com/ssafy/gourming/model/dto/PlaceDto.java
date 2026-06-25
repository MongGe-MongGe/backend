package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryResponse;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PlaceDto {

	// 프론트에서 선택한 장소를 검증할 때 사용하는 요청 정보
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceRequest {

		@NotBlank
		private String id;

		@NotBlank
		private String name;

		@NotBlank
		private String x;

		@NotBlank
		private String y;

		private String roadAddressName;
		private String categoryName;
	}

	// places 테이블 조회 및 저장에 사용하는 객체
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceEntity {

		private String id;
		private String name;
		private String categoryName;
		private String categoryGroupCode;
		private String roadAddressName;
		private String x;
		private String y;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}

	// 장소 상세 조회 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceDetailResponse {

		private String id;
		private String name;
		private String categoryName;
		private String categoryGroupCode;
		private String roadAddressName;
		private String x;
		private String y;
		private PlaceReviewSummaryResponse reviewSummary;
		
		@com.fasterxml.jackson.annotation.JsonProperty("isSaved")
		private boolean isSaved;
	}
}
