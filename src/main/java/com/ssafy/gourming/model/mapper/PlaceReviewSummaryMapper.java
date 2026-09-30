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
}
