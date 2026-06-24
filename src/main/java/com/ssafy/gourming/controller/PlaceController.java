package com.ssafy.gourming.controller;

import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceDetailResponse;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryBulkRefreshResponse;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryResponse;
import com.ssafy.gourming.model.service.PlaceReviewSummaryService;
import com.ssafy.gourming.model.service.PlaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

	private final PlaceService placeService;
	private final PlaceReviewSummaryService placeReviewSummaryService;

	@PostMapping
	public ResponseEntity<PlaceDetailResponse> checkOrCreatePlace(
		@Valid @RequestBody PlaceRequest request
	) {
		PlaceEntity place = placeService.findOrCreatePlace(request);
		if (place == null) {
			throw new NoSuchElementException("Place not found: " + request.getId());
		}

		// 공개 장소 검증/저장 API에서는 AI를 호출하지 않는다.
		// 관리자가 PUT으로 미리 생성해 둔 COMPLETED 요약만 함께 반환한다.
		PlaceReviewSummaryResponse reviewSummary =
			placeReviewSummaryService.getSummary(place.getId());

		return ResponseEntity.ok(toPlaceDetailResponse(place, reviewSummary));
	}

	@PutMapping("/summary")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<PlaceReviewSummaryBulkRefreshResponse> refreshAllSummaries() {
		return ResponseEntity.ok(placeReviewSummaryService.refreshAllSummaries());
	}

	@PutMapping("/summary/{placeId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<PlaceReviewSummaryResponse> refreshSummary(
		@PathVariable String placeId
	) {
		return ResponseEntity.ok(placeReviewSummaryService.refreshSummary(placeId));
	}

	private PlaceDetailResponse toPlaceDetailResponse(
		PlaceEntity place,
		PlaceReviewSummaryResponse reviewSummary
	) {
		PlaceDetailResponse response = new PlaceDetailResponse();
		response.setId(place.getId());
		response.setName(place.getName());
		response.setCategoryName(place.getCategoryName());
		response.setCategoryGroupCode(place.getCategoryGroupCode());
		response.setRoadAddressName(place.getRoadAddressName());
		response.setX(place.getX());
		response.setY(place.getY());
		response.setReviewSummary(reviewSummary);
		return response;
	}
}
