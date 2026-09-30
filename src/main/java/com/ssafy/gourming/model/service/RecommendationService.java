package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;

import com.ssafy.gourming.model.dto.RecommendationDto.RecommendationPageResponse;

public interface RecommendationService {

	// 사용자 벡터와 최근 리뷰 벡터의 유사도로 정렬한 추천 피드. 행동이 없는 사용자는 인기 피드를 반환한다.
	// asOf가 없으면 현재 시각을 기준으로 계산하고, 사용한 asOf를 응답에 담는다.
	// 다음 페이지 요청에 같은 asOf를 보내면 그 사이의 좋아요·저장·새 리뷰가 순위에 반영되지 않는다.
	RecommendationPageResponse getRecommendedReviews(String userId, int page, int size, LocalDateTime asOf);
}
