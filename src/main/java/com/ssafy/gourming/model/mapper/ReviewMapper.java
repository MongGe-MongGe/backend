package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;

@Mapper
public interface ReviewMapper {

	// 리뷰를 저장한다.
	int insertReview(ReviewEntity review);

	// 수정·삭제 권한 확인에 필요한 내부 엔티티를 조회한다.
	ReviewEntity selectReviewEntityById(String reviewId);

	// 장소, 작성자, 좋아요/댓글 수를 포함한 리뷰 상세 정보를 조회한다.
	ReviewResponse selectReviewById(
		@Param("reviewId") String reviewId,
		@Param("viewerId") String viewerId
	);

	// 작성자 본인 리뷰만 수정한다.
	int updateReview(ReviewEntity review);

	// 작성자 본인 리뷰만 삭제한다.
	int deleteReview(
		@Param("reviewId") String reviewId,
		@Param("userId") String userId
	);

	// 특정 장소의 리뷰 목록을 최신순으로 조회한다.
	List<ReviewResponse> selectReviewsByPlace(
		@Param("placeId") String placeId,
		@Param("viewerId") String viewerId,
		@Param("offset") long offset,
		@Param("size") int size
	);

	long countReviewsByPlace(String placeId);

	// 특정 사용자가 작성한 리뷰 목록을 최신순으로 조회한다.
	List<ReviewResponse> selectReviewsByUser(
		@Param("userId") String userId,
		@Param("viewerId") String viewerId,
		@Param("offset") long offset,
		@Param("size") int size
	);

	long countReviewsByUser(String userId);
}
