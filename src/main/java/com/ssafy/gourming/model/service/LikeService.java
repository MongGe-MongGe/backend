package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.LikeDto.LikeResponse;

public interface LikeService {

	// 리뷰 좋아요를 등록한다.
	LikeResponse likeReview(String userId, String reviewId);

	// 리뷰 좋아요를 취소한다.
	LikeResponse unlikeReview(String userId, String reviewId);
}
