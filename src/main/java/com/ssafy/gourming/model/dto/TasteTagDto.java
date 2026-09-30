package com.ssafy.gourming.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TasteTagDto {

	// taste_tags 테이블과 매핑되는 row
	@Getter
	@Setter
	@NoArgsConstructor
	public static class TasteTagRow {

		private String code;
		private String label;
		private String description;
		private String category;
		private float[] embedding;
		private String embedderVersion;
		private boolean active;
		private int sortOrder;
	}
}
