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

	// 인증된 사용자가 선택한 장소에 리뷰를 작성한다.
	@PostMapping("/reviews")
	public ResponseEntity<ReviewResponse> createReview(
		@AuthenticationPrincipal String userId,
		@Valid @RequestBody ReviewCreateRequest request
	) {
		ReviewResponse response = reviewService.createReview(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// 리뷰 상세 조회는 공개 API로 제공한다.
	@GetMapping("/reviews/{reviewId}")
	public ResponseEntity<ReviewResponse> getReview(
		@AuthenticationPrincipal String viewerId,
		@PathVariable String reviewId
	) {
		return ResponseEntity.ok(reviewService.getReview(reviewId, normalizeViewerId(viewerId)));
	}

	// 인증된 작성자 본인의 리뷰만 수정할 수 있다.
	@PutMapping("/reviews/{reviewId}")
	public ResponseEntity<ReviewResponse> updateReview(
		@AuthenticationPrincipal String userId,
		@PathVariable String reviewId,
		@Valid @RequestBody ReviewUpdateRequest request
	) {
		return ResponseEntity.ok(reviewService.updateReview(userId, reviewId, request));
	}

	// 인증된 작성자 본인의 리뷰만 삭제할 수 있다.
	@DeleteMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> deleteReview(
		@AuthenticationPrincipal String userId,
		@PathVariable String reviewId
	) {
		reviewService.deleteReview(userId, reviewId);
		return ResponseEntity.noContent().build();
	}

	// 장소 상세 화면에서 사용할 리뷰 목록을 공개 조회한다.
	@GetMapping("/places/{placeId}/reviews")
	public ResponseEntity<ReviewPageResponse> getReviewsByPlace(
		@AuthenticationPrincipal String viewerId,
		@PathVariable String placeId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(
			reviewService.getReviewsByPlace(placeId, normalizeViewerId(viewerId), page, size)
		);
	}

	// 사용자 프로필 화면에서 사용할 작성 리뷰 목록을 공개 조회한다.
	@GetMapping("/users/{userId}/reviews")
	public ResponseEntity<ReviewPageResponse> getReviewsByUser(
		@AuthenticationPrincipal String viewerId,
		@PathVariable String userId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(
			reviewService.getReviewsByUser(userId, normalizeViewerId(viewerId), page, size)
		);
	}

	// MVP 피드는 인증된 사용자가 작성한 리뷰 목록으로 제공한다.
	@GetMapping("/users/me/feeds")
	public ResponseEntity<ReviewPageResponse> getMyFeeds(
		@AuthenticationPrincipal String userId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(reviewService.getMyFeeds(userId, page, size));
	}

	private String normalizeViewerId(String viewerId) {
		if (viewerId == null || "anonymousUser".equals(viewerId)) {
			return null;
		}
		return viewerId;
	}
}
