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
}
