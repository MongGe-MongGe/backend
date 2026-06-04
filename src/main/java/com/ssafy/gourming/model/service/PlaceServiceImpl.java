package com.ssafy.gourming.model.service;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.dto.PlaceDto;
import com.ssafy.gourming.model.dto.PlaceRequestDto;
import com.ssafy.gourming.model.mapper.PlaceMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService{
	private final PlaceMapper pMapper;

	@Override
	public PlaceDto findOrCreatePlace(PlaceRequestDto placeRequestDto) {
		String id = placeRequestDto.getId();
		PlaceDto place = pMapper.selectPlaceById(id);
		if(place != null) {
			return place;
		}
		return place;
	}

	@Override
	public PlaceDto getPlace(String id) {
		// TODO Auto-generated method stub
		return pMapper.selectPlaceById(id);
	}
	
	
}
