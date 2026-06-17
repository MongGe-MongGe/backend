package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;

@Mapper
public interface ReviewMapper {

	int insertReview(ReviewEntity review);

	ReviewEntity selectReviewEntityById(String reviewId);

	ReviewResponse selectReviewById(String reviewId);

	int updateReview(ReviewEntity review);

	int deleteReview(
		@Param("reviewId") String reviewId,
		@Param("userId") String userId
	);

	List<ReviewResponse> selectReviewsByPlace(
		@Param("placeId") String placeId,
		@Param("offset") long offset,
		@Param("size") int size
	);

	long countReviewsByPlace(String placeId);

	List<ReviewResponse> selectReviewsByUser(
		@Param("userId") String userId,
		@Param("offset") long offset,
		@Param("size") int size
	);

	long countReviewsByUser(String userId);
}
