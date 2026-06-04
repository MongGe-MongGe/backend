package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.client.KakaoLocalClient;
import com.ssafy.gourming.model.dto.KakaoPlaceDto;
import com.ssafy.gourming.model.dto.PlaceDto;
import com.ssafy.gourming.model.dto.PlaceRequestDto;
import com.ssafy.gourming.model.mapper.PlaceMapper;

@ExtendWith(MockitoExtension.class)
class PlaceServiceImplTest {

	@Mock
	private PlaceMapper placeMapper;

	@Mock
	private KakaoLocalClient kakaoClient;

	@InjectMocks
	private PlaceServiceImpl placeService;

	@Test
	//DB에 장소가 이미 있으면 카카오 API를 호출하지 않고 기존 장소 반환
	void findOrCreatePlaceReturnsExistingPlaceWithoutCallingKakaoApi() {
		PlaceRequestDto request = createPlaceRequest("kakao-place-001", "성수 파스타하우스");
		PlaceDto existingPlace = createPlace("kakao-place-001", "성수 파스타하우스", "FD6", "서울 성동구 연무장길 12");

		when(placeMapper.selectPlaceById("kakao-place-001")).thenReturn(existingPlace);

		PlaceDto result = placeService.findOrCreatePlace(request);

		assertThat(result).isSameAs(existingPlace);
		verify(kakaoClient, never()).searchPlaceByKeyword(any(), any(), any());
		verify(placeMapper, never()).insertPlace(any());
	}

	@Test
	//DB에 없으면 카카오 결과 중 id가 일치하는 장소를 저장하고 저장된 장소 반환
	void findOrCreatePlaceSavesMatchingKakaoPlaceWhenPlaceDoesNotExist() {
		PlaceRequestDto request = createPlaceRequest("kakao-place-001", "성수 파스타하우스");
		KakaoPlaceDto otherKakaoPlace = createKakaoPlace("kakao-place-999", "다른 식당", "FD6", "서울 성동구 다른길 1", null);
		KakaoPlaceDto matchingKakaoPlace = createKakaoPlace(
			"kakao-place-001",
			"성수 파스타하우스",
			"FD6",
			"서울 성동구 연무장길 12",
			"서울 성동구 성수동"
		);
		PlaceDto savedPlace = createPlace("kakao-place-001", "성수 파스타하우스", "FD6", "서울 성동구 연무장길 12");

		when(placeMapper.selectPlaceById("kakao-place-001")).thenReturn(null, savedPlace);
		when(kakaoClient.searchPlaceByKeyword("성수 파스타하우스", "127.056123", "37.544321"))
			.thenReturn(List.of(otherKakaoPlace, matchingKakaoPlace));

		PlaceDto result = placeService.findOrCreatePlace(request);

		ArgumentCaptor<PlaceDto> placeCaptor = ArgumentCaptor.forClass(PlaceDto.class);
		verify(placeMapper).insertPlace(placeCaptor.capture());
		PlaceDto insertedPlace = placeCaptor.getValue();

		assertThat(insertedPlace.getId()).isEqualTo("kakao-place-001");
		assertThat(insertedPlace.getName()).isEqualTo("성수 파스타하우스");
		assertThat(insertedPlace.getCategoryName()).isEqualTo("FD6");
		assertThat(insertedPlace.getRoadAddressName()).isEqualTo("서울 성동구 연무장길 12");
		assertThat(insertedPlace.getX()).isEqualTo("127.056123");
		assertThat(insertedPlace.getY()).isEqualTo("37.544321");
		assertThat(result).isSameAs(savedPlace);
	}

	@Test
	//roadAddressName이 비어 있으면 addressName을 대체 주소로 사용
	void findOrCreatePlaceUsesAddressNameWhenRoadAddressNameIsBlank() {
		PlaceRequestDto request = createPlaceRequest("kakao-place-001", "성수 파스타하우스");
		KakaoPlaceDto matchingKakaoPlace = createKakaoPlace(
			"kakao-place-001",
			"성수 파스타하우스",
			"FD6",
			"",
			"서울 성동구 성수동"
		);
		PlaceDto savedPlace = createPlace("kakao-place-001", "성수 파스타하우스", "FD6", "서울 성동구 성수동");

		when(placeMapper.selectPlaceById("kakao-place-001")).thenReturn(null, savedPlace);
		when(kakaoClient.searchPlaceByKeyword("성수 파스타하우스", "127.056123", "37.544321"))
			.thenReturn(List.of(matchingKakaoPlace));

		PlaceDto result = placeService.findOrCreatePlace(request);

		ArgumentCaptor<PlaceDto> placeCaptor = ArgumentCaptor.forClass(PlaceDto.class);
		verify(placeMapper).insertPlace(placeCaptor.capture());

		assertThat(placeCaptor.getValue().getRoadAddressName()).isEqualTo("서울 성동구 성수동");
		assertThat(result).isSameAs(savedPlace);
	}

	@Test
	//카카오 결과에 일치하는 장소가 없으면 insert하지 않고 null 반환
	void findOrCreatePlaceReturnsNullWhenKakaoResultDoesNotContainMatchingPlace() {
		PlaceRequestDto request = createPlaceRequest("kakao-place-001", "성수 파스타하우스");
		KakaoPlaceDto otherKakaoPlace = createKakaoPlace("kakao-place-999", "다른 식당", "FD6", "서울 성동구 다른길 1", null);

		when(placeMapper.selectPlaceById("kakao-place-001")).thenReturn(null);
		when(kakaoClient.searchPlaceByKeyword("성수 파스타하우스", "127.056123", "37.544321"))
			.thenReturn(List.of(otherKakaoPlace));

		PlaceDto result = placeService.findOrCreatePlace(request);

		assertThat(result).isNull();
		verify(placeMapper, never()).insertPlace(any());
	}

	private PlaceRequestDto createPlaceRequest(String id, String name) {
		PlaceRequestDto request = new PlaceRequestDto();
		request.setId(id);
		request.setName(name);
		request.setX("127.056123");
		request.setY("37.544321");
		return request;
	}

	private PlaceDto createPlace(String id, String name, String categoryName, String roadAddressName) {
		PlaceDto place = new PlaceDto();
		place.setId(id);
		place.setName(name);
		place.setCategoryName(categoryName);
		place.setRoadAddressName(roadAddressName);
		place.setX("127.056123");
		place.setY("37.544321");
		return place;
	}

	private KakaoPlaceDto createKakaoPlace(
		String id,
		String name,
		String categoryName,
		String roadAddressName,
		String addressName
	) {
		KakaoPlaceDto kakaoPlace = new KakaoPlaceDto();
		kakaoPlace.setId(id);
		kakaoPlace.setName(name);
		kakaoPlace.setCategoryName(categoryName);
		kakaoPlace.setRoadAddressName(roadAddressName);
		kakaoPlace.setAddressName(addressName);
		kakaoPlace.setX("127.056123");
		kakaoPlace.setY("37.544321");
		return kakaoPlace;
	}
}
