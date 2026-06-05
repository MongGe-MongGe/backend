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
	
	// 카카오 API 응답을 DB 저장 형식으로 변환한다.
	private PlaceEntity toPlaceEntity(KakaoPlaceDto kakaoPlace) {
	    PlaceEntity place = new PlaceEntity();
	    place.setId(kakaoPlace.getId());
	    place.setName(kakaoPlace.getName());
	    place.setCategoryName(kakaoPlace.getCategoryName());
	    // 도로명 주소가 없으면 지번 주소를 대신 저장한다.
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

		// 이미 저장된 장소라면 외부 API를 호출하지 않는다.
		PlaceEntity place = placeMapper.selectPlaceById(id);
		if(place != null) {
			return place;
		}
		
		List<KakaoPlaceDto> kakaoPlaces = kakaoClient.searchPlaceByKeyword(
				placeRequestDto.getName(), placeRequestDto.getX(), placeRequestDto.getY());
		
		// 프론트에서 전달받은 카카오 장소 ID와 일치하는 결과만 저장한다.
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
