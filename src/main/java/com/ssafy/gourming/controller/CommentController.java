package com.ssafy.gourming.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.CommentDto.CommentCreateRequest;
import com.ssafy.gourming.model.dto.CommentDto.CommentPageResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentUpdateRequest;
import com.ssafy.gourming.model.service.CommentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	// 인증된 사용자가 특정 리뷰에 댓글을 작성한다.
	@PostMapping("/reviews/{reviewId}/comments")
	public ResponseEntity<CommentResponse> createComment(
		@AuthenticationPrincipal String userId,
		@PathVariable String reviewId,
		@Valid @RequestBody CommentCreateRequest request
	) {
		CommentResponse response = commentService.createComment(userId, reviewId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// 특정 리뷰의 댓글 목록을 오래된 순으로 공개 조회한다.
	@GetMapping("/reviews/{reviewId}/comments")
	public ResponseEntity<CommentPageResponse> getCommentsByReview(
		@PathVariable String reviewId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(commentService.getCommentsByReview(reviewId, page, size));
	}

	// 인증된 작성자 본인의 댓글만 수정한다.
	@PutMapping("/comments/{commentId}")
	public ResponseEntity<CommentResponse> updateComment(
		@AuthenticationPrincipal String userId,
		@PathVariable String commentId,
		@Valid @RequestBody CommentUpdateRequest request
	) {
		return ResponseEntity.ok(commentService.updateComment(userId, commentId, request));
	}

	// 인증된 작성자 본인의 댓글만 삭제한다.
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(
		@AuthenticationPrincipal String userId,
		@PathVariable String commentId
	) {
		commentService.deleteComment(userId, commentId);
		return ResponseEntity.noContent().build();
	}
}
