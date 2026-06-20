package com.ssafy.gourming.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LikeMapper {

	// 리뷰 좋아요를 저장한다.
	int insertLike(
		@Param("userId") String userId,
		@Param("reviewId") String reviewId
	);

	// 지정한 사용자의 리뷰 좋아요를 삭제한다.
	int deleteLike(
		@Param("userId") String userId,
		@Param("reviewId") String reviewId
	);

	// 지정한 사용자가 해당 리뷰를 좋아요했는지 조회한다.
	boolean existsLike(
		@Param("userId") String userId,
		@Param("reviewId") String reviewId
	);

	long countLikesByReview(String reviewId);
}
