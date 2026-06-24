package com.ssafy.gourming.model.client;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;

@Component
@ConditionalOnProperty(
	name = "place-summary.ai.enabled",
	havingValue = "false",
	matchIfMissing = true
)
public class FakePlaceReviewSummarizer implements PlaceReviewSummarizer {

	@Override
	public PlaceReviewSummaryGenerateResult summarize(
		String placeId,
		List<ReviewSummarySourceRow> reviews
	) {
		List<ReviewSummarySourceRow> safeReviews =
			reviews == null ? Collections.emptyList() : reviews;

		PlaceReviewSummaryGenerateResult result = new PlaceReviewSummaryGenerateResult();
		result.setReviewCount(safeReviews.size());
		result.setLastReviewUpdatedAt(findLastReviewUpdatedAt(safeReviews));

		if (safeReviews.isEmpty()) {
			result.setSummary("아직 작성된 리뷰가 없습니다.");
			result.setPositivePoints(List.of());
			result.setNegativePoints(List.of());
			result.setRecommendedFor(List.of());
			result.setKeywords(List.of());
			return result;
		}

		result.setSummary("최근 리뷰를 기준으로 전반적인 만족도와 방문 경험을 요약했습니다.");
		result.setPositivePoints(createPositivePoints(safeReviews));
		result.setNegativePoints(createNegativePoints(safeReviews));
		result.setRecommendedFor(List.of("가볍게 방문하기 좋은 장소"));
		result.setKeywords(List.of("리뷰", "방문", "장소"));
		return result;
	}

	private List<String> createPositivePoints(List<ReviewSummarySourceRow> reviews) {
		boolean hasPositiveReview = reviews.stream()
			.map(ReviewSummarySourceRow::getRatingScore)
			.filter(Objects::nonNull)
			.anyMatch(ratingScore -> ratingScore >= 4);

		if (!hasPositiveReview) {
			return List.of();
		}
		return List.of("만족도가 높은 편이에요");
	}

	private List<String> createNegativePoints(List<ReviewSummarySourceRow> reviews) {
		boolean hasNegativeReview = reviews.stream()
			.map(ReviewSummarySourceRow::getRatingScore)
			.filter(Objects::nonNull)
			.anyMatch(ratingScore -> ratingScore <= 2);

		if (!hasNegativeReview) {
			return List.of();
		}
		return List.of("일부 아쉬운 평가가 있어요");
	}

	private LocalDateTime findLastReviewUpdatedAt(List<ReviewSummarySourceRow> reviews) {
		return reviews.stream()
			.map(ReviewSummarySourceRow::getUpdatedAt)
			.filter(Objects::nonNull)
			.max(LocalDateTime::compareTo)
			.orElse(null);
	}
}
