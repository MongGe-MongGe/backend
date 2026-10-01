package com.ssafy.gourming.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.ReviewImpressionDto.ReviewImpressionRequest;
import com.ssafy.gourming.model.service.ReviewImpressionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews/impressions")
@RequiredArgsConstructor
public class ReviewImpressionController {

	private final ReviewImpressionService reviewImpressionService;

	// 피드에서 실제로 화면에 보인 리뷰를 기록한다. 추천 피드가 최근에 본 리뷰를 제외하는 데 쓴다.
	@PostMapping
	public ResponseEntity<Void> recordImpressions(
		@AuthenticationPrincipal String userId,
		@Valid @RequestBody ReviewImpressionRequest request
	) {
		reviewImpressionService.record(userId, request.getReviewIds());
		return ResponseEntity.noContent().build();
	}
}
