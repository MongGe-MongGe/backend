package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.PlaceDto;
import com.ssafy.gourming.model.dto.PlaceRequestDto;

public interface PlaceService {

	PlaceDto findOrCreatePlace(PlaceRequestDto placeRequestDto);

	PlaceDto getPlace(String id);
}
