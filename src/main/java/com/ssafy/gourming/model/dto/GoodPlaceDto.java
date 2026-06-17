package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GoodPlaceDto {

	// good_places 테이블과 매핑되는 내부 엔티티
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GoodPlaceEntity {

		private String id;
		private String userId;
		private String groupId;
		private String placeId;
		private LocalDateTime createdAt;
	}

	// 그룹에 저장할 장소 정보
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GoodPlaceCreateRequest {

		@Valid
		@NotNull(message = "장소 정보는 필수입니다.")
		private PlaceRequest place;
	}

	// 맛집 저장 결과 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GoodPlaceResponse {

		private String id;
		private String groupId;
		private String placeId;
		private LocalDateTime createdAt;
	}

	// 그룹 맛집 목록에서 사용하는 장소 요약 정보
	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceSummary {

		private String id;
		private String name;
		private String categoryName;
		private String roadAddressName;
		private String x;
		private String y;
	}

	// 그룹 맛집과 장소 정보를 함께 제공하는 조회 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GoodPlaceDetailResponse {

		private String id;
		private String groupId;
		private PlaceSummary place;
		private LocalDateTime createdAt;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class GoodPlacePageResponse {

		private List<GoodPlaceDetailResponse> content;
		private int page;
		private int size;
		private long totalElements;
		private int totalPages;
		private boolean first;
		private boolean last;
	}
}
