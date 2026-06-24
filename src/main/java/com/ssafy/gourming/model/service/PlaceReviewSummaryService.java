package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryBulkRefreshResponse;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryResponse;

public interface PlaceReviewSummaryService {

	// 저장된 COMPLETED 요약만 반환한다. 없거나 생성 중/실패 상태이면 null을 반환한다.
	PlaceReviewSummaryResponse getSummary(String placeId);

	// 지정한 장소의 리뷰 요약을 현재 리뷰 기준으로 강제 갱신한다.
	PlaceReviewSummaryResponse refreshSummary(String placeId);

	// 전체 장소의 리뷰 요약을 강제 갱신하고 성공/실패 집계를 반환한다.
	PlaceReviewSummaryBulkRefreshResponse refreshAllSummaries();
}
