package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserTasteDto {

	// 사용자 행동 하나. 벡터 출처와 가중치를 함께 담는다.
	// createdAt은 지금은 쓰지 않지만, 시간 감쇠를 넣을 때 조회 쿼리를 바꾸지 않도록 함께 읽는다.
	@Getter
	@Setter
	@NoArgsConstructor
	public static class UserTasteEvidenceRow {

		public static final String SOURCE_PLACE_SAVE = "PLACE_SAVE";
		public static final String SOURCE_LIKE = "LIKE";
		public static final String SOURCE_OWN_REVIEW = "OWN_REVIEW";

		private String source;
		private float[] embedding;
		private double weight;
		private LocalDateTime createdAt;
	}

	// 요청 시 계산한 사용자 벡터. DB에 저장하지 않는다.
	@Getter
	@AllArgsConstructor
	public static class UserVector {

		private final float[] vector;
		private final int evidenceCount;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class UserTasteTagScore {

		private String code;
		private String label;
		private String category;
		private double score;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class UserTasteProfileResponse {

		private int evidenceCount;
		private double confidence;
		private List<UserTasteTagScore> topTags;
	}
}
