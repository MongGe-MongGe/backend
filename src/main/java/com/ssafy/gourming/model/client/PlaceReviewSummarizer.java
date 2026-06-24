package com.ssafy.gourming.model.client;

import java.util.List;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;

public interface PlaceReviewSummarizer {

	// 장소 리뷰 목록을 기반으로 AI 리뷰 요약 결과를 생성한다.
	PlaceReviewSummaryGenerateResult summarize(
		String placeId,
		List<ReviewSummarySourceRow> reviews
	);
}
