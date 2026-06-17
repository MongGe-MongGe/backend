package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.ReviewDto.ReviewCreateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewUpdateRequest;

public interface ReviewService {

	ReviewResponse createReview(String userId, ReviewCreateRequest request);

	ReviewResponse getReview(String reviewId);

	ReviewResponse updateReview(
		String userId,
		String reviewId,
		ReviewUpdateRequest request
	);

	void deleteReview(String userId, String reviewId);

	ReviewPageResponse getReviewsByPlace(String placeId, int page, int size);

	ReviewPageResponse getReviewsByUser(String userId, int page, int size);

	ReviewPageResponse getMyFeeds(String userId, int page, int size);
}
