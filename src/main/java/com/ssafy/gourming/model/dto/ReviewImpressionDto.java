package com.ssafy.gourming.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ReviewImpressionDto {

	// 프론트가 화면에 실제로 보인 리뷰 카드 ID를 묶어서 보낸다.
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ReviewImpressionRequest {

		@NotEmpty(message = "reviewIds는 비어 있을 수 없습니다.")
		@Size(max = 100, message = "reviewIds는 한 번에 100개까지 보낼 수 있습니다.")
		private List<String> reviewIds;
	}
}
