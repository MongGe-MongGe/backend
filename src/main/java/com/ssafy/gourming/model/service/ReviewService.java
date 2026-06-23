package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.ReviewDto.ReviewCreateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewUpdateRequest;

public interface ReviewService {

	// 장소 검증 후 리뷰를 생성하고 첨부 이미지를 확정한다.
	ReviewResponse createReview(String userId, ReviewCreateRequest request);

	// 리뷰 ID로 단건 상세 정보를 조회한다.
	ReviewResponse getReview(String reviewId, String viewerId);

	// 작성자 본인만 리뷰 내용을 수정할 수 있다.
	ReviewResponse updateReview(
		String userId,
		String reviewId,
		ReviewUpdateRequest request
	);

	// 작성자 본인만 리뷰를 삭제할 수 있다.
	void deleteReview(String userId, String reviewId);

	// 특정 장소에 작성된 리뷰 목록을 조회한다.
	ReviewPageResponse getReviewsByPlace(String placeId, String viewerId, int page, int size);

	// 특정 사용자가 작성한 리뷰 목록을 조회한다.
	ReviewPageResponse getReviewsByUser(String userId, String viewerId, int page, int size);

	//  내가 작성한 리뷰 목록을 조회한다.
	ReviewPageResponse getMyFeeds(String userId, int page, int size);

	// 인기피드 집계 결과를 조회한다.
	ReviewPageResponse getPopularReviews(String viewerId, int page, int size);

	// 모든 리뷰 목록을 조회한다.
	ReviewPageResponse getAllReviews(String viewerId, int page, int size);
}
