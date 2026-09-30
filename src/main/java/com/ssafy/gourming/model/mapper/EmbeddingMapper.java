package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingTargetRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.PlaceEmbeddingRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.ReviewEmbeddingRow;

@Mapper
public interface EmbeddingMapper {

	// 벡터가 없거나, 벡터 생성 후 수정됐거나, 버전이 다른 리뷰를 전부 조회한다.
	List<EmbeddingTargetRow> selectEmbeddingTargets(@Param("embedderVersion") String embedderVersion);

	int upsertReviewEmbeddings(@Param("rows") List<ReviewEmbeddingRow> rows);

	// 현재 버전 벡터를 가진 리뷰가 하나 이상 있는 장소 ID
	List<String> selectPlaceIdsWithEmbeddedReviews(@Param("embedderVersion") String embedderVersion);

	List<ReviewEmbeddingRow> selectReviewEmbeddingsByPlaceId(
		@Param("placeId") String placeId,
		@Param("embedderVersion") String embedderVersion
	);

	// 리뷰가 하나도 없고, 장소 벡터가 없거나 버전이 다르거나 CATEGORY가 아닌 장소 ID
	List<String> selectPlaceIdsNeedingCategoryEmbedding(@Param("embedderVersion") String embedderVersion);

	int upsertPlaceEmbedding(PlaceEmbeddingRow row);
}
