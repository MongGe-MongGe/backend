package com.ssafy.gourming.model.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.config.RecommendationProperties;
import com.ssafy.gourming.model.client.TextEmbedder;
import com.ssafy.gourming.model.dto.RecommendationDto.RecommendationCandidateRow;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.UserTasteDto.UserVector;
import com.ssafy.gourming.model.mapper.RecommendationMapper;
import com.ssafy.gourming.model.mapper.ReviewMapper;
import com.ssafy.gourming.util.VectorMath;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

	private final RecommendationMapper recommendationMapper;
	private final ReviewMapper reviewMapper;
	private final UserTasteService userTasteService;
	private final ReviewService reviewService;
	private final TextEmbedder textEmbedder;
	private final RecommendationProperties properties;

	@Override
	public ReviewPageResponse getRecommendedReviews(String userId, int page, int size) {
		validatePageRequest(page, size);

		UserVector userVector = userTasteService.computeUserVector(userId);
		if (userVector == null) {
			// 콜드 스타트: 응답 형식이 같은 인기 피드로 대체한다.
			log.debug("추천 피드 콜드 스타트, 인기 피드로 대체. userId={}", userId);
			return reviewService.getPopularReviews(userId, page, size);
		}

		// ponytail: 요청마다 후보 전체의 코사인을 계산한다. 사용자·리뷰가 수천 단위가 되면
		// 인기피드처럼 배치로 추천 결과를 미리 저장하는 방식으로 전환한다.
		List<RecommendationCandidateRow> candidates = recommendationMapper.selectCandidates(
			userId, textEmbedder.version(), properties.getCandidateSize(), null);
		List<ScoredCandidate> scored = new ArrayList<>(candidates.size());
		for (RecommendationCandidateRow candidate : candidates) {
			// 배치가 모델을 바꾸는 중이면 길이가 다른 벡터가 섞일 수 있다. 코사인이 예외를 던지므로 건너뛴다.
			if (candidate.getEmbedding() == null
				|| candidate.getEmbedding().length != userVector.getVector().length) {
				continue;
			}
			scored.add(new ScoredCandidate(
				candidate, VectorMath.cosine(userVector.getVector(), candidate.getEmbedding())));
		}
		// 점수 → 최신순 → 리뷰 ID 순으로 완전히 정해 두어 같은 데이터면 페이지마다 같은 순서가 나오게 한다.
		scored.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed()
			.thenComparing(s -> s.candidate().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(s -> s.candidate().getReviewId(), Comparator.reverseOrder()));

		List<String> orderedIds = applyPlaceLimit(scored);
		long totalElements = orderedIds.size();
		int fromIndex = (int) Math.min((long) page * size, totalElements);
		int toIndex = (int) Math.min(fromIndex + (long) size, totalElements);
		List<String> pageIds = orderedIds.subList(fromIndex, toIndex);

		// 빈 목록으로 IN () 쿼리를 만들면 SQL 오류가 나므로 호출하지 않는다.
		List<ReviewResponse> content = pageIds.isEmpty()
			? List.of()
			: reorder(reviewMapper.selectReviewsByIds(pageIds, userId), pageIds);

		return createPageResponse(content, page, size, totalElements);
	}

	private List<String> applyPlaceLimit(List<ScoredCandidate> scored) {
		// 점수순으로 훑으며 같은 장소 리뷰가 이미 maxPerPlace개면 건너뛴다.
		int maxPerPlace = Math.max(1, properties.getMaxPerPlace());
		Map<String, Integer> countByPlace = new HashMap<>();
		List<String> ids = new ArrayList<>();
		for (ScoredCandidate item : scored) {
			String placeId = item.candidate().getPlaceId();
			int count = countByPlace.getOrDefault(placeId, 0);
			if (count >= maxPerPlace) {
				continue;
			}
			countByPlace.put(placeId, count + 1);
			ids.add(item.candidate().getReviewId());
		}
		return ids;
	}

	private List<ReviewResponse> reorder(List<ReviewResponse> reviews, List<String> orderedIds) {
		// IN 조회는 순서를 보장하지 않으므로 점수 순서대로 다시 배열한다.
		Map<String, ReviewResponse> byId = new HashMap<>();
		for (ReviewResponse review : reviews) {
			byId.put(review.getId(), review);
		}
		List<ReviewResponse> ordered = new ArrayList<>(orderedIds.size());
		for (String id : orderedIds) {
			ReviewResponse review = byId.get(id);
			if (review != null) {
				ordered.add(review);
			}
		}
		return ordered;
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must be zero or greater");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("Size must be between 1 and 100");
		}
	}

	private ReviewPageResponse createPageResponse(
		List<ReviewResponse> content,
		int page,
		int size,
		long totalElements
	) {
		int totalPages = (int) ((totalElements + size - 1) / size);
		ReviewPageResponse response = new ReviewPageResponse();
		response.setContent(content);
		response.setPage(page);
		response.setSize(size);
		response.setTotalElements(totalElements);
		response.setTotalPages(totalPages);
		response.setFirst(page == 0);
		response.setLast(totalPages == 0 || page >= totalPages - 1);
		return response;
	}

	private record ScoredCandidate(RecommendationCandidateRow candidate, double score) {
	}
}
