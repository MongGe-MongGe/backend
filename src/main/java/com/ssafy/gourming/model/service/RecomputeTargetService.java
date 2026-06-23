package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.gourming.model.dto.RecomputeTargetDto.RecomputeTarget;
import com.ssafy.gourming.model.dto.RecomputeTargetType;

public interface RecomputeTargetService {

	// 리뷰 본문 분석 대상 등록
	void registerReviewTarget(String reviewId);

	// 장소 특징/요약 재계산 대상 등록
	void registerPlaceTarget(String placeId);

	// 사용자 미식 성향 재계산 대상 등록
	void registerUserTasteTarget(String userId);

	// 사용자 추천만 갱신할 대상 등록
	void registerUserRecommendationTarget(String userId);

	// 배치 cutoff 이전 대상 조회
	List<RecomputeTarget> selectTargets(
		RecomputeTargetType targetType,
		LocalDateTime cutoffAt,
		int limit
	);

	// 배치 cutoff 이전 대상 수 조회
	long countTargets(RecomputeTargetType targetType, LocalDateTime cutoffAt);

	// 성공 처리된 대상 조건부 삭제
	int deleteProcessedTarget(
		RecomputeTargetType targetType,
		String targetId,
		LocalDateTime cutoffAt
	);
}
