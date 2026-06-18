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

import com.ssafy.gourming.model.dto.ReviewDto.ReviewCreateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewUpdateRequest;
import com.ssafy.gourming.model.service.ReviewService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping("/reviews")
	public ResponseEntity<ReviewResponse> createReview(
		@AuthenticationPrincipal String userId,
		@Valid @RequestBody ReviewCreateRequest request
	) {
		ReviewResponse response = reviewService.createReview(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/reviews/{reviewId}")
	public ResponseEntity<ReviewResponse> getReview(
		@PathVariable String reviewId
	) {
		return ResponseEntity.ok(reviewService.getReview(reviewId));
	}

	@PutMapping("/reviews/{reviewId}")
	public ResponseEntity<ReviewResponse> updateReview(
		@AuthenticationPrincipal String userId,
		@PathVariable String reviewId,
		@Valid @RequestBody ReviewUpdateRequest request
	) {
		return ResponseEntity.ok(reviewService.updateReview(userId, reviewId, request));
	}

	@DeleteMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> deleteReview(
		@AuthenticationPrincipal String userId,
		@PathVariable String reviewId
	) {
		reviewService.deleteReview(userId, reviewId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/places/{placeId}/reviews")
	public ResponseEntity<ReviewPageResponse> getReviewsByPlace(
		@PathVariable String placeId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(reviewService.getReviewsByPlace(placeId, page, size));
	}

	@GetMapping("/users/{userId}/reviews")
	public ResponseEntity<ReviewPageResponse> getReviewsByUser(
		@PathVariable String userId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(reviewService.getReviewsByUser(userId, page, size));
	}

	@GetMapping("/users/me/feeds")
	public ResponseEntity<ReviewPageResponse> getMyFeeds(
		@AuthenticationPrincipal String userId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(reviewService.getMyFeeds(userId, page, size));
	}
}
