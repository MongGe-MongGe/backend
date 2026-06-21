package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.CommentDto.CommentCreateRequest;
import com.ssafy.gourming.model.dto.CommentDto.CommentPageResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentUpdateRequest;

public interface CommentService {

	// 리뷰에 댓글을 작성한다.
	CommentResponse createComment(
		String userId,
		String reviewId,
		CommentCreateRequest request
	);

	// 특정 리뷰의 댓글 목록을 조회한다.
	CommentPageResponse getCommentsByReview(String reviewId, int page, int size);

	// 작성자 본인 댓글을 수정한다.
	CommentResponse updateComment(
		String userId,
		String commentId,
		CommentUpdateRequest request
	);

	// 작성자 본인 댓글을 삭제한다.
	void deleteComment(String userId, String commentId);
}
