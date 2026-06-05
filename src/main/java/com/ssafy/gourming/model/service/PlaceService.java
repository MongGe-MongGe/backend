package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;

public interface PlaceService {

	PlaceEntity findOrCreatePlace(PlaceRequest placeRequestDto);

	PlaceEntity getPlace(String id);
}
