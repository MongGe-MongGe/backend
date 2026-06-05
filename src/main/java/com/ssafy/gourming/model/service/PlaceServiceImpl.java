package com.ssafy.gourming.model.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.client.KakaoLocalClient;
import com.ssafy.gourming.model.dto.KakaoPlaceDto;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.mapper.PlaceMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService{
	private final PlaceMapper placeMapper;
	private final KakaoLocalClient kakaoClient;
	
	private PlaceEntity toPlaceEntity(KakaoPlaceDto kakaoPlace) {
	    PlaceEntity place = new PlaceEntity();
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
	public PlaceEntity findOrCreatePlace(PlaceRequest placeRequestDto) {
		String id = placeRequestDto.getId();
		PlaceEntity place = placeMapper.selectPlaceById(id);
		if(place != null) {
			return place;
		}
		
		List<KakaoPlaceDto> kakaoPlaces = kakaoClient.searchPlaceByKeyword(
				placeRequestDto.getName(), placeRequestDto.getX(), placeRequestDto.getY());
		
		for(KakaoPlaceDto kakaoPlace:kakaoPlaces) {
			if(id.equals(kakaoPlace.getId())) {
				place = toPlaceEntity(kakaoPlace);
				placeMapper.insertPlace(place);
				break;
			}
		}
		
		return placeMapper.selectPlaceById(id);
	}

	@Override
	public PlaceEntity getPlace(String id) {
		return placeMapper.selectPlaceById(id);
	}
	
	
}
