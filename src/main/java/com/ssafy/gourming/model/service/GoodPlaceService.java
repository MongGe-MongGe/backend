package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceCreateRequest;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlacePageResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceResponse;

public interface GoodPlaceService {

	// 로그인 사용자의 그룹에 맛집 저장
	GoodPlaceResponse createGoodPlace(
		String userId,
		String groupId,
		GoodPlaceCreateRequest request
	);

	// 경로의 userId에 해당하는 사용자의 그룹 맛집 공개 조회
	GoodPlacePageResponse getGroupGoodPlaces(
		String targetUserId,
		String groupId,
		int page,
		int size
	);

	// 로그인 사용자의 특정 그룹에서 맛집 삭제
	void deleteGoodPlaceFromGroup(
		String userId,
		String groupId,
		String placeId
	);
}
