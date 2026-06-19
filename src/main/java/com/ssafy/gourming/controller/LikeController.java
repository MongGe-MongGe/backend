package com.ssafy.gourming.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.LikeDto.LikeResponse;
import com.ssafy.gourming.model.service.LikeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class LikeController {

	private final LikeService likeService;

	// 인증된 사용자가 리뷰에 좋아요를 등록한다.
	@PostMapping("/{reviewId}/likes")
	public ResponseEntity<LikeResponse> likeReview(
		@AuthenticationPrincipal String userId,
		@PathVariable String reviewId
	) {
		return ResponseEntity.ok(likeService.likeReview(userId, reviewId));
	}

	// 인증된 사용자가 리뷰 좋아요를 취소한다.
	@DeleteMapping("/{reviewId}/likes")
	public ResponseEntity<LikeResponse> unlikeReview(
		@AuthenticationPrincipal String userId,
		@PathVariable String reviewId
	) {
		return ResponseEntity.ok(likeService.unlikeReview(userId, reviewId));
	}
}
