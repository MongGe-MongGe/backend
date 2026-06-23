package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TasteTagDto {

	// taste_tags 테이블과 매핑되는 미식 태그 사전 엔티티
	@Getter
	@Setter
	@NoArgsConstructor
	public static class TasteTag {

		private Long id;
		private String code;
		private String name;
		private String type;
		private String description;
		private boolean active;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}
}
