package com.ssafy.gourming.controller;

import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

	// 프론트에서 선택한 카카오 장소를 검증·저장한 뒤 장소 상세와 리뷰 요약을 반환한다.
	@PostMapping
	public ResponseEntity<PlaceDetailResponse> checkOrCreatePlace(
		@Valid @RequestBody PlaceRequest request
	) {
		PlaceEntity place = placeService.findOrCreatePlace(request);
		if (place == null) {
			throw new NoSuchElementException("Place not found: " + request.getId());
		}

		// 저장된 요약이 없으면 lazy 생성한다.
		// 생성 실패 시 서비스가 null을 반환하므로 장소 상세 응답은 유지된다.
		PlaceReviewSummaryResponse reviewSummary =
			placeReviewSummaryService.getOrCreateSummary(place.getId());

		return ResponseEntity.ok(toPlaceDetailResponse(place, reviewSummary));
	}

	// 장소 상세 정보와 리뷰 요약을 함께 공개 조회한다.
	@GetMapping("/{placeId}")
	public ResponseEntity<PlaceDetailResponse> getPlace(
		@PathVariable String placeId
	) {
		PlaceEntity place = placeService.getPlace(placeId);
		if (place == null) {
			throw new NoSuchElementException("Place not found: " + placeId);
		}

		// 저장된 요약이 없으면 lazy 생성한다.
		// 생성 실패 시 서비스가 null을 반환하므로 장소 상세 응답은 유지된다.
		PlaceReviewSummaryResponse reviewSummary =
			placeReviewSummaryService.getOrCreateSummary(placeId);

		return ResponseEntity.ok(toPlaceDetailResponse(place, reviewSummary));
	}

	// 관리자용: 전체 장소의 리뷰 요약을 강제 갱신한다.
	@PutMapping("/summary")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<PlaceReviewSummaryBulkRefreshResponse> refreshAllSummaries() {
		return ResponseEntity.ok(placeReviewSummaryService.refreshAllSummaries());
	}

	// 관리자용: 단일 장소의 리뷰 요약을 강제 갱신한다.
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
