package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.dto.PopularFeedDto.PopularReviewScoreRow;
import com.ssafy.gourming.model.mapper.PopularFeedMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("인기피드 서비스 Mock 단위 테스트")
class PopularFeedServiceMockTest {

	private static final int WINDOW_DAYS = 7;
	private static final String FIRST_REVIEW_ID = "review-1";
	private static final String SECOND_REVIEW_ID = "review-2";

	@Mock
	private PopularFeedMapper popularFeedMapper;

	@InjectMocks
	private PopularFeedServiceImpl popularFeedService;

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("인기 점수 계산 결과에 순위를 부여하고 기존 집계를 교체한다")
	void refreshPopularScores() {
		List<PopularReviewScoreRow> calculatedScores = List.of(
			createScoreRow(FIRST_REVIEW_ID, 12.5),
			createScoreRow(SECOND_REVIEW_ID, 8.0)
		);
		when(popularFeedMapper.calculatePopularScores(WINDOW_DAYS)).thenReturn(calculatedScores);

		popularFeedService.refreshPopularScores(WINDOW_DAYS);

		ArgumentCaptor<List<PopularReviewScoreRow>> scoresCaptor = ArgumentCaptor.forClass(List.class);
		verify(popularFeedMapper).insertPopularScores(scoresCaptor.capture());

		List<PopularReviewScoreRow> insertedScores = scoresCaptor.getValue();
		assertThat(insertedScores).hasSize(2);
		assertThat(insertedScores)
			.extracting(PopularReviewScoreRow::getRankNo)
			.containsExactly(1, 2);
		assertThat(insertedScores)
			.extracting(PopularReviewScoreRow::getWindowDays)
			.containsExactly(WINDOW_DAYS, WINDOW_DAYS);
		assertThat(insertedScores)
			.extracting(PopularReviewScoreRow::getCalculatedAt)
			.doesNotContainNull();

		InOrder inOrder = inOrder(popularFeedMapper);
		inOrder.verify(popularFeedMapper).calculatePopularScores(WINDOW_DAYS);
		inOrder.verify(popularFeedMapper).deleteScoresByWindowDays(WINDOW_DAYS);
		inOrder.verify(popularFeedMapper).insertPopularScores(insertedScores);
	}

	@Test
	@DisplayName("계산 결과가 없어도 기존 window 집계는 삭제하고 저장은 생략한다")
	void refreshPopularScoresWithEmptyResult() {
		when(popularFeedMapper.calculatePopularScores(WINDOW_DAYS)).thenReturn(List.of());

		popularFeedService.refreshPopularScores(WINDOW_DAYS);

		verify(popularFeedMapper).deleteScoresByWindowDays(WINDOW_DAYS);
		verify(popularFeedMapper, never()).insertPopularScores(anyList());
	}

	@Test
	@DisplayName("집계 window는 1일 이상이어야 한다")
	void refreshPopularScoresWithInvalidWindowDaysFails() {
		assertThatThrownBy(() -> popularFeedService.refreshPopularScores(0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Window days must be positive");

		verify(popularFeedMapper, never()).calculatePopularScores(0);
		verify(popularFeedMapper, never()).deleteScoresByWindowDays(0);
		verify(popularFeedMapper, never()).insertPopularScores(anyList());
	}

	private PopularReviewScoreRow createScoreRow(String reviewId, double score) {
		PopularReviewScoreRow row = new PopularReviewScoreRow();
		row.setReviewId(reviewId);
		row.setScore(score);
		return row;
	}
}
