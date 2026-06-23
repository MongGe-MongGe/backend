package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.dto.RecomputeTargetDto.RecomputeTarget;
import com.ssafy.gourming.model.dto.RecomputeTargetType;
import com.ssafy.gourming.model.mapper.RecomputeTargetMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("재계산 대상 서비스 Mock 단위 테스트")
class RecomputeTargetServiceMockTest {

	private static final String TARGET_ID = "target-1";
	private static final LocalDateTime CUTOFF_AT = LocalDateTime.of(2026, 6, 23, 0, 0);

	@Mock
	private RecomputeTargetMapper recomputeTargetMapper;

	@InjectMocks
	private RecomputeTargetServiceImpl recomputeTargetService;

	@Test
	@DisplayName("리뷰 대상은 요청 시각 갱신 방식으로 등록한다")
	void registerReviewTarget() {
		recomputeTargetService.registerReviewTarget(TARGET_ID);

		verify(recomputeTargetMapper).insertOrRefreshTarget(
			RecomputeTargetType.REVIEW,
			TARGET_ID
		);
		verify(recomputeTargetMapper, never()).insertTargetIfAbsent(
			RecomputeTargetType.REVIEW,
			TARGET_ID
		);
	}

	@Test
	@DisplayName("장소 대상은 요청 시각 갱신 방식으로 등록한다")
	void registerPlaceTarget() {
		recomputeTargetService.registerPlaceTarget(TARGET_ID);

		verify(recomputeTargetMapper).insertOrRefreshTarget(
			RecomputeTargetType.PLACE,
			TARGET_ID
		);
	}

	@Test
	@DisplayName("사용자 성향 대상은 요청 시각 갱신 방식으로 등록한다")
	void registerUserTasteTarget() {
		recomputeTargetService.registerUserTasteTarget(TARGET_ID);

		verify(recomputeTargetMapper).insertOrRefreshTarget(
			RecomputeTargetType.USER_TASTE,
			TARGET_ID
		);
	}

	@Test
	@DisplayName("사용자 추천 대상은 최초 1회 등록 방식으로 등록한다")
	void registerUserRecommendationTarget() {
		recomputeTargetService.registerUserRecommendationTarget(TARGET_ID);

		verify(recomputeTargetMapper).insertTargetIfAbsent(
			RecomputeTargetType.USER_RECOMMENDATION,
			TARGET_ID
		);
		verify(recomputeTargetMapper, never()).insertOrRefreshTarget(
			RecomputeTargetType.USER_RECOMMENDATION,
			TARGET_ID
		);
	}

	@Test
	@DisplayName("cutoff 이전 대상 목록을 조회한다")
	void selectTargets() {
		List<RecomputeTarget> targets = List.of(new RecomputeTarget());
		when(recomputeTargetMapper.selectTargets(
			RecomputeTargetType.REVIEW,
			CUTOFF_AT,
			100
		)).thenReturn(targets);

		List<RecomputeTarget> result = recomputeTargetService.selectTargets(
			RecomputeTargetType.REVIEW,
			CUTOFF_AT,
			100
		);

		assertThat(result).isSameAs(targets);
	}

	@Test
	@DisplayName("cutoff 이전 대상 수를 조회한다")
	void countTargets() {
		when(recomputeTargetMapper.countTargets(
			RecomputeTargetType.PLACE,
			CUTOFF_AT
		)).thenReturn(3L);

		long result = recomputeTargetService.countTargets(
			RecomputeTargetType.PLACE,
			CUTOFF_AT
		);

		assertThat(result).isEqualTo(3);
	}

	@Test
	@DisplayName("성공 처리된 대상을 조건부 삭제한다")
	void deleteProcessedTarget() {
		when(recomputeTargetMapper.deleteProcessedTarget(
			RecomputeTargetType.REVIEW,
			TARGET_ID,
			CUTOFF_AT
		)).thenReturn(1);

		int result = recomputeTargetService.deleteProcessedTarget(
			RecomputeTargetType.REVIEW,
			TARGET_ID,
			CUTOFF_AT
		);

		assertThat(result).isEqualTo(1);
	}

	@Test
	@DisplayName("빈 대상 ID는 등록하지 않는다")
	void registerBlankTargetIdFails() {
		assertThatThrownBy(() -> recomputeTargetService.registerReviewTarget(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Target id is required");

		verify(recomputeTargetMapper, never()).insertOrRefreshTarget(
			RecomputeTargetType.REVIEW,
			" "
		);
	}

	@Test
	@DisplayName("조회 limit은 1 이상이어야 한다")
	void selectTargetsWithInvalidLimitFails() {
		assertThatThrownBy(() -> recomputeTargetService.selectTargets(
			RecomputeTargetType.REVIEW,
			CUTOFF_AT,
			0
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Limit must be greater than zero");
	}
}
