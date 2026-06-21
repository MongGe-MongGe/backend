package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.CommentDto.CommentEntity;
import com.ssafy.gourming.model.dto.CommentDto.CommentResponse;

@Mapper
public interface CommentMapper {

	// 리뷰 댓글을 저장한다.
	int insertComment(CommentEntity comment);

	// 수정·삭제 권한 확인에 필요한 내부 엔티티를 조회한다.
	CommentEntity selectCommentEntityById(String commentId);

	// 작성자 정보를 포함한 댓글 응답을 조회한다.
	CommentResponse selectCommentById(String commentId);

	// 특정 리뷰의 댓글 목록을 오래된 순으로 조회한다.
	List<CommentResponse> selectCommentsByReview(
		@Param("reviewId") String reviewId,
		@Param("offset") long offset,
		@Param("size") int size
	);

	long countCommentsByReview(String reviewId);

	// 작성자 본인 댓글만 수정한다.
	int updateComment(CommentEntity comment);

	// 작성자 본인 댓글만 삭제한다.
	int deleteComment(
		@Param("commentId") String commentId,
		@Param("userId") String userId
	);
}
