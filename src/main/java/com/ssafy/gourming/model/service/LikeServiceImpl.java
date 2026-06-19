package com.ssafy.gourming.model.service;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.LikeDto.LikeResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.mapper.LikeMapper;
import com.ssafy.gourming.model.mapper.ReviewMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

	private final LikeMapper likeMapper;
	private final ReviewMapper reviewMapper;

	@Override
	@Transactional
	public LikeResponse likeReview(String userId, String reviewId) {
		validateReviewExists(reviewId);
		likeMapper.insertLike(userId, reviewId);

		return createLikeResponse(reviewId, true);
	}

	@Override
	@Transactional
	public LikeResponse unlikeReview(String userId, String reviewId) {
		validateReviewExists(reviewId);
		likeMapper.deleteLike(userId, reviewId);

		return createLikeResponse(reviewId, false);
	}

	private void validateReviewExists(String reviewId) {
		ReviewEntity review = reviewMapper.selectReviewEntityById(reviewId);
		if (review == null) {
			throw new NoSuchElementException("Review not found: " + reviewId);
		}
	}

	private LikeResponse createLikeResponse(String reviewId, boolean likedByMe) {
		LikeResponse response = new LikeResponse();
		response.setReviewId(reviewId);
		response.setLikedByMe(likedByMe);
		response.setLikeCount(likeMapper.countLikesByReview(reviewId));
		return response;
	}
}
