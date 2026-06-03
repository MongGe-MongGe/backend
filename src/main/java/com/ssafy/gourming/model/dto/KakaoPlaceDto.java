package com.ssafy.gourming.model.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KakaoPlaceDto {

	@JsonProperty("id")
	private String id;

	@JsonProperty("place_name")
	private String name;

	@JsonProperty("category_group_code")
	private String categoryName;

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

		private List<KakaoPlaceDto> documents;
	}
}
