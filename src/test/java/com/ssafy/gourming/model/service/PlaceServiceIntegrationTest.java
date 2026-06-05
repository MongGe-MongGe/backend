package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.mapper.PlaceMapper;

@SpringBootTest
@EnabledIfSystemProperty(named = "run.place.integration", matches = "true")
@DisplayName("장소 서비스 통합 테스트")
class PlaceServiceIntegrationTest {

	private static final String TEST_PLACE_ID = "35026031";

	@Autowired
	private PlaceService placeService;

	@Autowired
	private PlaceMapper placeMapper;

	@BeforeEach
	void setUp() {
		placeMapper.deletePlaceById(TEST_PLACE_ID);
	}

	@AfterEach
	void tearDown() {
		placeMapper.deletePlaceById(TEST_PLACE_ID);
	}

	@Test
	@DisplayName("카카오 API에서 검증한 장소를 실제 DB에 저장하고 반환한다")
	void findOrCreatePlaceCallsKakaoApiAndSavesPlaceToDatabase() {
		PlaceRequest request = new PlaceRequest();
		request.setId(TEST_PLACE_ID);
		request.setName("스타벅스 강남R점");
		request.setX("127.028443419181");
		request.setY("37.4976744709989");

		PlaceEntity result = placeService.findOrCreatePlace(request);
		PlaceEntity selectedPlace = placeMapper.selectPlaceById(TEST_PLACE_ID);
		
		assertThat(result).isNotNull();

		System.out.printf(
			"Saved place: id=%s, name=%s, category=%s, roadAddress=%s, x=%s, y=%s%n",
			result.getId(),
			result.getName(),
			result.getCategoryName(),
			result.getRoadAddressName(),
			result.getX(),
			result.getY()
		);

		
		assertThat(result.getId()).isEqualTo(TEST_PLACE_ID);
		assertThat(result.getName()).isNotBlank();
		assertThat(result.getCategoryName()).isIn("FD6", "CE7");
		assertThat(result.getX()).isNotBlank();
		assertThat(result.getY()).isNotBlank();
		assertThat(selectedPlace).isNotNull();
		assertThat(selectedPlace.getId()).isEqualTo(TEST_PLACE_ID);
	}
}
