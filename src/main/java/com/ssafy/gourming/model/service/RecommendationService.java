package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;

public interface RecommendationService {

	// 사용자 벡터와 최근 리뷰 벡터의 유사도로 정렬한 추천 피드. 행동이 없는 사용자는 인기 피드를 반환한다.
	ReviewPageResponse getRecommendedReviews(String userId, int page, int size);
}
