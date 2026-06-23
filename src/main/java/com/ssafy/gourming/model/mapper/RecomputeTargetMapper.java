package com.ssafy.gourming.model.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.RecomputeTargetDto.RecomputeTarget;
import com.ssafy.gourming.model.dto.RecomputeTargetType;

@Mapper
public interface RecomputeTargetMapper {

	// 리뷰/좋아요/저장처럼 실제 변경이 생긴 대상은 요청 시각을 갱신한다.
	int insertOrRefreshTarget(
		@Param("targetType") RecomputeTargetType targetType,
		@Param("targetId") String targetId
	);

	// 로그인 추천 요청처럼 반복 이벤트가 많을 때는 기존 요청 시각을 유지한다.
	int insertTargetIfAbsent(
		@Param("targetType") RecomputeTargetType targetType,
		@Param("targetId") String targetId
	);

	// 배치 cutoff 이전에 요청된 대상을 오래된 순서로 가져온다.
	List<RecomputeTarget> selectTargets(
		@Param("targetType") RecomputeTargetType targetType,
		@Param("cutoffAt") LocalDateTime cutoffAt,
		@Param("limit") int limit
	);

	// 후속 단계 backlog 규모를 로그로 확인하기 위해 대상 수를 센다.
	long countTargets(
		@Param("targetType") RecomputeTargetType targetType,
		@Param("cutoffAt") LocalDateTime cutoffAt
	);

	// 배치 중 새 요청이 들어온 행은 requested_at이 cutoff보다 커져 삭제되지 않는다.
	int deleteProcessedTarget(
		@Param("targetType") RecomputeTargetType targetType,
		@Param("targetId") String targetId,
		@Param("cutoffAt") LocalDateTime cutoffAt
	);
}
