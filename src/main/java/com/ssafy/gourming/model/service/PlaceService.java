package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;

public interface PlaceService {

	// DB에 장소가 없으면 카카오 API로 검증한 뒤 저장한다.
	PlaceEntity findOrCreatePlace(PlaceRequest placeRequestDto);

	PlaceEntity getPlace(String id);
}
