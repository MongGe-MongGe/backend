package com.ssafy.gourming.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceCreateRequest;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlacePageResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceResponse;
import com.ssafy.gourming.model.service.GoodPlaceService;
import com.ssafy.gourming.security.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class GoodPlaceController {

	private final GoodPlaceService goodPlaceService;

	// 그룹 맛집 목록은 공개 API이며 userId는 조회 대상 사용자를 의미한다.
	@GetMapping("/{userId}/groups/{groupId}/good-places")
	public ResponseEntity<GoodPlacePageResponse> getGroupGoodPlaces(
		@PathVariable String userId,
		@PathVariable String groupId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ResponseEntity.ok(
			goodPlaceService.getGroupGoodPlaces(userId, groupId, page, size)
		);
	}

	// 맛집 변경 API는 인증 principal의 사용자 ID를 사용한다.
	@PostMapping("/me/groups/{groupId}/good-places")
	public ResponseEntity<GoodPlaceResponse> createGoodPlace(
		@AuthenticationPrincipal LoginUser loginUser,
		@PathVariable String groupId,
		@Valid @RequestBody GoodPlaceCreateRequest request
	) {
		GoodPlaceResponse response =
			goodPlaceService.createGoodPlace(loginUser.id(), groupId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// places 데이터는 유지하고 해당 그룹의 저장 관계만 삭제한다.
	@DeleteMapping("/me/groups/{groupId}/good-places/{placeId}")
	public ResponseEntity<Void> deleteGoodPlaceFromGroup(
		@AuthenticationPrincipal LoginUser loginUser,
		@PathVariable String groupId,
		@PathVariable String placeId
	) {
		goodPlaceService.deleteGoodPlaceFromGroup(loginUser.id(), groupId, placeId);
		return ResponseEntity.noContent().build();
	}
}
