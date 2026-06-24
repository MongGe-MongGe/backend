package com.ssafy.gourming.model.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;

@DisplayName("Fake 장소 리뷰 요약 생성기 테스트")
class FakePlaceReviewSummarizerTest {

	private final FakePlaceReviewSummarizer summarizer = new FakePlaceReviewSummarizer();

	@Test
	@DisplayName("리뷰가 없으면 빈 요약을 반환한다")
	void summarizeWithoutReviews() {
		PlaceReviewSummaryGenerateResult result =
			summarizer.summarize("test-place-id", List.of());

		assertThat(result.getSummary()).isEqualTo("아직 작성된 리뷰가 없습니다.");
		assertThat(result.getPositivePoints()).isEmpty();
		assertThat(result.getNegativePoints()).isEmpty();
		assertThat(result.getRecommendedFor()).isEmpty();
		assertThat(result.getKeywords()).isEmpty();
		assertThat(result.getReviewCount()).isZero();
		assertThat(result.getLastReviewUpdatedAt()).isNull();
	}

	@Test
	@DisplayName("긍정 리뷰가 있으면 긍정 포인트를 반환한다")
	void summarizeWithPositiveReview() {
		ReviewSummarySourceRow review = createReviewSource(
			"review-1",
			"맛있고 분위기도 좋아요.",
			5,
			LocalDateTime.of(2026, 6, 24, 10, 0)
		);

		PlaceReviewSummaryGenerateResult result =
			summarizer.summarize("test-place-id", List.of(review));

		assertThat(result.getSummary())
			.isEqualTo("최근 리뷰를 기준으로 전반적인 만족도와 방문 경험을 요약했습니다.");
		assertThat(result.getPositivePoints()).containsExactly("만족도가 높은 편이에요");
		assertThat(result.getNegativePoints()).isEmpty();
		assertThat(result.getRecommendedFor()).containsExactly("가볍게 방문하기 좋은 장소");
		assertThat(result.getKeywords()).containsExactly("리뷰", "방문", "장소");
		assertThat(result.getReviewCount()).isEqualTo(1);
		assertThat(result.getLastReviewUpdatedAt())
			.isEqualTo(LocalDateTime.of(2026, 6, 24, 10, 0));
	}

	@Test
	@DisplayName("부정 리뷰가 있으면 부정 포인트를 반환한다")
	void summarizeWithNegativeReview() {
		ReviewSummarySourceRow review = createReviewSource(
			"review-1",
			"아쉬운 점이 있었어요.",
			2,
			LocalDateTime.of(2026, 6, 24, 10, 0)
		);

		PlaceReviewSummaryGenerateResult result =
			summarizer.summarize("test-place-id", List.of(review));

		assertThat(result.getPositivePoints()).isEmpty();
		assertThat(result.getNegativePoints()).containsExactly("일부 아쉬운 평가가 있어요");
		assertThat(result.getReviewCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("가장 최신 리뷰 수정 시간을 lastReviewUpdatedAt으로 반환한다")
	void summarizeUsesLatestUpdatedAt() {
		ReviewSummarySourceRow olderReview = createReviewSource(
			"review-1",
			"첫 번째 리뷰",
			4,
			LocalDateTime.of(2026, 6, 24, 10, 0)
		);
		ReviewSummarySourceRow newerReview = createReviewSource(
			"review-2",
			"두 번째 리뷰",
			3,
			LocalDateTime.of(2026, 6, 24, 12, 0)
		);

		PlaceReviewSummaryGenerateResult result =
			summarizer.summarize("test-place-id", List.of(olderReview, newerReview));

		assertThat(result.getReviewCount()).isEqualTo(2);
		assertThat(result.getLastReviewUpdatedAt())
			.isEqualTo(LocalDateTime.of(2026, 6, 24, 12, 0));
	}

	private ReviewSummarySourceRow createReviewSource(
		String reviewId,
		String content,
		Integer ratingScore,
		LocalDateTime updatedAt
	) {
		ReviewSummarySourceRow review = new ReviewSummarySourceRow();
		review.setReviewId(reviewId);
		review.setContent(content);
		review.setRatingScore(ratingScore);
		review.setVisitedAt(LocalDate.of(2026, 6, 24));
		review.setCreatedAt(updatedAt.minusHours(1));
		review.setUpdatedAt(updatedAt);
		return review;
	}
}
