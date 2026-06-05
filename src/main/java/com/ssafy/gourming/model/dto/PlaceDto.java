package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PlaceDto {

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

	@Getter
	@Setter
	@NoArgsConstructor
	public static class PlaceEntity {

		private String id;
		private String name;
		private String categoryName;
		private String roadAddressName;
		private String x;
		private String y;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}
}
