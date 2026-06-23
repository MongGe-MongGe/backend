package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.RecomputeTargetDto.RecomputeTarget;
import com.ssafy.gourming.model.dto.RecomputeTargetType;
import com.ssafy.gourming.model.mapper.RecomputeTargetMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecomputeTargetServiceImpl implements RecomputeTargetService {

	private final RecomputeTargetMapper recomputeTargetMapper;

	@Override
	@Transactional
	public void registerReviewTarget(String reviewId) {
		registerRefreshTarget(RecomputeTargetType.REVIEW, reviewId);
	}

	@Override
	@Transactional
	public void registerPlaceTarget(String placeId) {
		registerRefreshTarget(RecomputeTargetType.PLACE, placeId);
	}

	@Override
	@Transactional
	public void registerUserTasteTarget(String userId) {
		registerRefreshTarget(RecomputeTargetType.USER_TASTE, userId);
	}

	@Override
	@Transactional
	public void registerUserRecommendationTarget(String userId) {
		validateTargetId(userId);
		recomputeTargetMapper.insertTargetIfAbsent(
			RecomputeTargetType.USER_RECOMMENDATION,
			userId
		);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RecomputeTarget> selectTargets(
		RecomputeTargetType targetType,
		LocalDateTime cutoffAt,
		int limit
	) {
		validateTargetType(targetType);
		validateCutoffAt(cutoffAt);
		validateLimit(limit);

		return recomputeTargetMapper.selectTargets(targetType, cutoffAt, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public long countTargets(RecomputeTargetType targetType, LocalDateTime cutoffAt) {
		validateTargetType(targetType);
		validateCutoffAt(cutoffAt);

		return recomputeTargetMapper.countTargets(targetType, cutoffAt);
	}

	@Override
	@Transactional
	public int deleteProcessedTarget(
		RecomputeTargetType targetType,
		String targetId,
		LocalDateTime cutoffAt
	) {
		validateTargetType(targetType);
		validateTargetId(targetId);
		validateCutoffAt(cutoffAt);

		return recomputeTargetMapper.deleteProcessedTarget(
			targetType,
			targetId,
			cutoffAt
		);
	}

	private void registerRefreshTarget(RecomputeTargetType targetType, String targetId) {
		validateTargetId(targetId);
		recomputeTargetMapper.insertOrRefreshTarget(targetType, targetId);
	}

	private void validateTargetType(RecomputeTargetType targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("Target type is required");
		}
	}

	private void validateTargetId(String targetId) {
		if (targetId == null || targetId.isBlank()) {
			throw new IllegalArgumentException("Target id is required");
		}
	}

	private void validateCutoffAt(LocalDateTime cutoffAt) {
		if (cutoffAt == null) {
			throw new IllegalArgumentException("Cutoff time is required");
		}
	}

	private void validateLimit(int limit) {
		if (limit < 1) {
			throw new IllegalArgumentException("Limit must be greater than zero");
		}
	}
}
