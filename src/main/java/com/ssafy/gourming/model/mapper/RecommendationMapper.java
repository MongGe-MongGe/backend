package com.ssafy.gourming.model.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.RecommendationDto.RecommendationCandidateRow;

@Mapper
public interface RecommendationMapper {

	// 본인 리뷰와 이미 좋아요한 리뷰를 제외하고, 현재 버전 벡터가 있는 최근 리뷰를 limit개 조회한다.
	// asOf가 있으면 그 시각 이전에 작성된 리뷰와 그 시각 이전의 좋아요만 기준으로 삼는다.
	List<RecommendationCandidateRow> selectCandidates(
		@Param("userId") String userId,
		@Param("embedderVersion") String embedderVersion,
		@Param("limit") int limit,
		@Param("asOf") LocalDateTime asOf
	);
}
