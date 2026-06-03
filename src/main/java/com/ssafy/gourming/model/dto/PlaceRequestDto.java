package com.ssafy.gourming.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaceRequestDto {

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
