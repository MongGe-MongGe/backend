package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaceDto {

	private String id;
	private String name;
	private String categoryName;
	private String roadAddressName;
	private String x;
	private String y;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
