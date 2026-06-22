package com.ssafy.gourming.model.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 카카오 Local API 장소 검색 결과
@Getter
@Setter
@NoArgsConstructor
public class KakaoPlaceDto {

	@JsonProperty("id")
	private String id;

	@JsonProperty("place_name")
	private String name;

	@JsonProperty("category_name")
	private String categoryName;

	@JsonProperty("category_group_code")
	private String categoryGroupCode;

	@JsonProperty("road_address_name")
	private String roadAddressName;

	@JsonProperty("address_name")
	private String addressName;

	private String x;
	private String y;

	@Getter
	@Setter
	@NoArgsConstructor
	public static class KeywordSearchResponse {

		// 카카오 API가 검색된 장소 목록을 documents 필드로 반환한다.
		private List<KakaoPlaceDto> documents;
	}
}
