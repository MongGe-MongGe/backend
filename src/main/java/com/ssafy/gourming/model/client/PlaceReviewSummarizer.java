package com.ssafy.gourming.model.client;

import java.util.List;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;
import com.ssafy.gourming.model.dto.TasteTagDto.TasteTagRow;

public interface PlaceReviewSummarizer {

	// 장소 리뷰 목록과 허용 태그 목록을 기반으로 AI 리뷰 요약 결과를 생성한다.
	// tagSentiments의 키는 allowedTags의 code만 허용한다.
	PlaceReviewSummaryGenerateResult summarize(
		String placeId,
		List<ReviewSummarySourceRow> reviews,
		List<TasteTagRow> allowedTags
	);
}
