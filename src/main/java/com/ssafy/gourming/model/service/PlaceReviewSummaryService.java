package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryBulkRefreshResponse;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryResponse;

public interface PlaceReviewSummaryService {

	// 저장된 요약이 있으면 반환하고, 없으면 현재 리뷰 기준으로 생성한다.
	// 생성 실패 시 장소 상세 조회를 막지 않도록 null을 반환한다.
	PlaceReviewSummaryResponse getOrCreateSummary(String placeId);

	// 지정한 장소의 리뷰 요약을 현재 리뷰 기준으로 강제 갱신한다.
	PlaceReviewSummaryResponse refreshSummary(String placeId);

	// 전체 장소의 리뷰 요약을 강제 갱신하고 성공/실패 집계를 반환한다.
	PlaceReviewSummaryBulkRefreshResponse refreshAllSummaries();
}
