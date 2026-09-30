package com.ssafy.gourming.model.client;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;
import com.ssafy.gourming.model.dto.TasteTagDto.TasteTagRow;

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
		List<ReviewSummarySourceRow> reviews,
		List<TasteTagRow> allowedTags
	) {
		// 테스트나 로컬 fallback 환경에서도 실제 AI 생성기와 같은 입력 계약을 사용한다.
		List<ReviewSummarySourceRow> safeReviews =
			reviews == null ? Collections.emptyList() : reviews;
		List<TasteTagRow> safeTags = allowedTags == null ? Collections.emptyList() : allowedTags;

		PlaceReviewSummaryGenerateResult result = new PlaceReviewSummaryGenerateResult();
		result.setReviewCount(safeReviews.size());
		result.setLastReviewUpdatedAt(findLastReviewUpdatedAt(safeReviews));

		// 리뷰가 없으면 AI 생성기와 동일하게 정상적인 빈 요약 결과를 반환한다.
		if (safeReviews.isEmpty()) {
			result.setSummary("아직 작성된 리뷰가 없습니다.");
			result.setPositivePoints(List.of());
			result.setNegativePoints(List.of());
			result.setRecommendedFor(List.of());
			result.setKeywords(List.of());
			result.setTagSentiments(new LinkedHashMap<>());
			return result;
		}

		// Fake 생성기는 외부 API를 호출하지 않고 테스트 가능한 고정 규칙으로 요약을 만든다.
		result.setSummary("최근 리뷰를 기준으로 전반적인 만족도와 방문 경험을 요약했습니다.");
		result.setPositivePoints(createPositivePoints(safeReviews));
		result.setNegativePoints(createNegativePoints(safeReviews));
		result.setRecommendedFor(List.of("가볍게 방문하기 좋은 장소"));
		result.setKeywords(List.of("리뷰", "방문", "장소"));
		result.setTagSentiments(createTagSentiments(safeReviews, safeTags));
		return result;
	}

	private List<String> createPositivePoints(List<ReviewSummarySourceRow> reviews) {
		// 별점 4점 이상 리뷰가 하나라도 있으면 긍정 포인트를 포함한다.
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
		// 별점 2점 이하 리뷰가 하나라도 있으면 부정 포인트를 포함한다.
		boolean hasNegativeReview = reviews.stream()
			.map(ReviewSummarySourceRow::getRatingScore)
			.filter(Objects::nonNull)
			.anyMatch(ratingScore -> ratingScore <= 2);

		if (!hasNegativeReview) {
			return List.of();
		}
		return List.of("일부 아쉬운 평가가 있어요");
	}

	// 리뷰 본문에 태그 라벨이 포함되면 0.7, "웨이팅"/"대기"가 포함되면 waiting을 -0.7로 둔다.
	private Map<String, Double> createTagSentiments(
		List<ReviewSummarySourceRow> reviews,
		List<TasteTagRow> tags
	) {
		String joined = reviews.stream()
			.map(ReviewSummarySourceRow::getContent)
			.filter(Objects::nonNull)
			.collect(Collectors.joining(" "));
		Map<String, Double> sentiments = new LinkedHashMap<>();
		for (TasteTagRow tag : tags) {
			if ("waiting".equals(tag.getCode())) {
				if (joined.contains("웨이팅") || joined.contains("대기")) {
					sentiments.put("waiting", -0.7);
				}
				continue;
			}
			if (tag.getLabel() != null && joined.contains(tag.getLabel())) {
				sentiments.put(tag.getCode(), 0.7);
			}
		}
		return sentiments;
	}

	private LocalDateTime findLastReviewUpdatedAt(List<ReviewSummarySourceRow> reviews) {
		// 실제 생성기와 동일하게 최신 리뷰 수정 시각을 결과에 담는다.
		return reviews.stream()
			.map(ReviewSummarySourceRow::getUpdatedAt)
			.filter(Objects::nonNull)
			.max(LocalDateTime::compareTo)
			.orElse(null);
	}
}
