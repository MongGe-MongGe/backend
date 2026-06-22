package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("장소 Mapper 테스트")
class PlaceMapperTest {

	private static final String TEST_PLACE_ID = "test-place-mapper-id";

	@Autowired
	private PlaceMapper placeMapper;

	@AfterEach
	void tearDown() {
		placeMapper.deletePlaceById(TEST_PLACE_ID);
	}

	@Test
	@DisplayName("장소를 저장한 뒤 ID로 조회한다")
	void insertPlaceAndSelectPlaceById() {
		placeMapper.deletePlaceById(TEST_PLACE_ID);

		PlaceEntity place = new PlaceEntity();
		place.setId(TEST_PLACE_ID);
		place.setName("테스트 식당");
		place.setCategoryName("음식점 > 한식");
		place.setCategoryGroupCode("FD6");
		place.setRoadAddressName("서울시 테스트구 테스트로 1");
		place.setX("127.000000");
		place.setY("37.000000");

		int insertedCount = placeMapper.insertPlace(place);
		PlaceEntity selectedPlace = placeMapper.selectPlaceById(TEST_PLACE_ID);

		assertThat(insertedCount).isEqualTo(1);
		assertThat(selectedPlace).isNotNull();
		assertThat(selectedPlace.getId()).isEqualTo(TEST_PLACE_ID);
		assertThat(selectedPlace.getName()).isEqualTo("테스트 식당");
		assertThat(selectedPlace.getCategoryName()).isEqualTo("음식점 > 한식");
		assertThat(selectedPlace.getCategoryGroupCode()).isEqualTo("FD6");
		assertThat(selectedPlace.getRoadAddressName()).isEqualTo("서울시 테스트구 테스트로 1");
		assertThat(selectedPlace.getX()).isEqualTo("127.000000");
		assertThat(selectedPlace.getY()).isEqualTo("37.000000");
	}
}
