package com.ssafy.gourming.model.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.CommentDto.CommentCreateRequest;
import com.ssafy.gourming.model.dto.CommentDto.CommentEntity;
import com.ssafy.gourming.model.dto.CommentDto.CommentPageResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentUpdateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.mapper.CommentMapper;
import com.ssafy.gourming.model.mapper.ReviewMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

	private final CommentMapper commentMapper;
	private final ReviewMapper reviewMapper;

	@Override
	@Transactional
	public CommentResponse createComment(
		String userId,
		String reviewId,
		CommentCreateRequest request
	) {
		getReviewEntity(reviewId);

		CommentEntity comment = new CommentEntity();
		comment.setId(UUID.randomUUID().toString());
		comment.setUserId(userId);
		comment.setReviewId(reviewId);
		comment.setContent(normalizeContent(request.getContent()));

		int insertedCount = commentMapper.insertComment(comment);
		if (insertedCount == 0) {
			throw new IllegalStateException("Failed to create comment");
		}

		CommentResponse response = commentMapper.selectCommentById(comment.getId());
		if (response == null) {
			throw new IllegalStateException("Failed to find created comment");
		}
		return response;
	}

	@Override
	public CommentPageResponse getCommentsByReview(String reviewId, int page, int size) {
		validatePageRequest(page, size);
		getReviewEntity(reviewId);

		long totalElements = commentMapper.countCommentsByReview(reviewId);
		long offset = (long)page * size;
		List<CommentResponse> content =
			commentMapper.selectCommentsByReview(reviewId, offset, size);

		return createPageResponse(content, page, size, totalElements);
	}

	@Override
	@Transactional
	public CommentResponse updateComment(
		String userId,
		String commentId,
		CommentUpdateRequest request
	) {
		CommentEntity comment = getCommentEntity(commentId);
		validateOwner(comment, userId);
		comment.setContent(normalizeContent(request.getContent()));

		int updatedCount = commentMapper.updateComment(comment);
		if (updatedCount == 0) {
			throw new IllegalStateException("Failed to update comment: " + commentId);
		}

		CommentResponse response = commentMapper.selectCommentById(commentId);
		if (response == null) {
			throw new IllegalStateException("Failed to find updated comment");
		}
		return response;
	}

	@Override
	@Transactional
	public void deleteComment(String userId, String commentId) {
		CommentEntity comment = getCommentEntity(commentId);
		validateOwner(comment, userId);

		int deletedCount = commentMapper.deleteComment(commentId, userId);
		if (deletedCount == 0) {
			throw new IllegalStateException("Failed to delete comment: " + commentId);
		}
	}

	private ReviewEntity getReviewEntity(String reviewId) {
		ReviewEntity review = reviewMapper.selectReviewEntityById(reviewId);
		if (review == null) {
			throw new NoSuchElementException("Review not found: " + reviewId);
		}
		return review;
	}

	private CommentEntity getCommentEntity(String commentId) {
		CommentEntity comment = commentMapper.selectCommentEntityById(commentId);
		if (comment == null) {
			throw new NoSuchElementException("Comment not found: " + commentId);
		}
		return comment;
	}

	private void validateOwner(CommentEntity comment, String userId) {
		if (!comment.getUserId().equals(userId)) {
			throw new SecurityException("Comment does not belong to user");
		}
	}

	private String normalizeContent(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("Comment content is required");
		}
		return content.strip();
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must be zero or greater");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("Size must be between 1 and 100");
		}
	}

	private CommentPageResponse createPageResponse(
		List<CommentResponse> content,
		int page,
		int size,
		long totalElements
	) {
		int totalPages = (int)((totalElements + size - 1) / size);

		CommentPageResponse response = new CommentPageResponse();
		response.setContent(content);
		response.setPage(page);
		response.setSize(size);
		response.setTotalElements(totalElements);
		response.setTotalPages(totalPages);
		response.setFirst(page == 0);
		response.setLast(totalPages == 0 || page >= totalPages - 1);
		return response;
	}
}
