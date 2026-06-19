package com.ssafy.gourming.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class LikeDto {

	// 리뷰 좋아요 등록 및 취소 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class LikeResponse {

		private String reviewId;
		private boolean likedByMe;
		private long likeCount;
	}
}
