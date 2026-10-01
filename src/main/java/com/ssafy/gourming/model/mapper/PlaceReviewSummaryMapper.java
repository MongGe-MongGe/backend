package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryEntity;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;

@Mapper
public interface PlaceReviewSummaryMapper {

	PlaceReviewSummaryEntity selectByPlaceId(String placeId);

	int upsert(PlaceReviewSummaryEntity summary);

	int markProcessing(
		@Param("placeId") String placeId,
		@Param("modelVersion") String modelVersion
	);

	int markFailed(
		@Param("placeId") String placeId,
		@Param("modelVersion") String modelVersion,
		@Param("errorMessage") String errorMessage
	);

	List<ReviewSummarySourceRow> selectReviewSourcesByPlaceId(String placeId);

	// 요약이 없거나, 요약 이후 리뷰가 수정된 장소 ID. 리뷰가 0개인 장소는 제외한다.
	List<String> selectStalePlaceIds();

	// 단일 장소가 요약 갱신 대상인지 selectStalePlaceIds와 같은 기준으로 판정한다.
	boolean isSummaryStale(String placeId);

	// 1분 안에 생성을 시작했거나(PROCESSING) 실패한(FAILED) 요약인지. 시각 비교는 DB 시계로 한다.
	boolean isRecentlyAttempted(String placeId);
}
