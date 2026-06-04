package com.ssafy.gourming.model.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.client.KakaoLocalClient;
import com.ssafy.gourming.model.dto.KakaoPlaceDto;
import com.ssafy.gourming.model.dto.PlaceDto;
import com.ssafy.gourming.model.dto.PlaceRequestDto;
import com.ssafy.gourming.model.mapper.PlaceMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService{
	private final PlaceMapper placeMapper;
	private final KakaoLocalClient kakaoClient;
	
	private PlaceDto toPlaceDto(KakaoPlaceDto kakaoPlace) {
	    PlaceDto place = new PlaceDto();
	    place.setId(kakaoPlace.getId());
	    place.setName(kakaoPlace.getName());
	    place.setCategoryName(kakaoPlace.getCategoryName());
	    place.setRoadAddressName(
	        kakaoPlace.getRoadAddressName() != null && !kakaoPlace.getRoadAddressName().isBlank()
	            ? kakaoPlace.getRoadAddressName()
	            : kakaoPlace.getAddressName()
	    );
	    place.setX(kakaoPlace.getX());
	    place.setY(kakaoPlace.getY());
	    return place;
	}

	@Override
	public PlaceDto findOrCreatePlace(PlaceRequestDto placeRequestDto) {
		String id = placeRequestDto.getId();
		PlaceDto place = placeMapper.selectPlaceById(id);
		if(place != null) {
			return place;
		}
		
		List<KakaoPlaceDto> kakaoPlaces = kakaoClient.searchPlaceByKeyword(
				placeRequestDto.getName(), placeRequestDto.getX(), placeRequestDto.getY());
		
		for(KakaoPlaceDto kakaoPlace:kakaoPlaces) {
			if(id.equals(kakaoPlace.getId())) {
				place = toPlaceDto(kakaoPlace);
				placeMapper.insertPlace(place);
				break;
			}
		}
		
		return placeMapper.selectPlaceById(id);
	}

	@Override
	public PlaceDto getPlace(String id) {
		return placeMapper.selectPlaceById(id);
	}
	
	
}
