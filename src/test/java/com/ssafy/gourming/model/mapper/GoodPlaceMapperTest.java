package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceDetailResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("맛집 Mapper 테스트")
class GoodPlaceMapperTest {

	private static final String TEST_USER_ID = "test-good-place-user-000000000001";
	private static final String OTHER_USER_ID = "test-good-place-user-000000000002";
	private static final String PLACE_ID = "test-good-place-001";
	private static final String OTHER_PLACE_ID = "test-good-place-002";

	@Autowired
	private GoodPlaceMapper goodPlaceMapper;

	@Autowired
	private GroupMapper groupMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertTestUser(TEST_USER_ID, "good-place-user-1@test.com", "@good_place_user_1");
		insertTestUser(OTHER_USER_ID, "good-place-user-2@test.com", "@good_place_user_2");
		insertTestPlace(PLACE_ID, "테스트 맛집");
		insertTestPlace(OTHER_PLACE_ID, "다른 테스트 맛집");
	}

	@Test
	@DisplayName("맛집을 그룹에 저장하고 조회한다")
	void insertAndSelectGoodPlace() {
		String groupId = insertGroup(TEST_USER_ID, "친구 추천");
		GoodPlaceEntity goodPlace = createGoodPlace(TEST_USER_ID, groupId, PLACE_ID);

		int insertedCount = goodPlaceMapper.insertGoodPlace(goodPlace);
		GoodPlaceEntity selectedGoodPlace =
			goodPlaceMapper.selectGoodPlace(TEST_USER_ID, groupId, PLACE_ID);

		assertThat(insertedCount).isEqualTo(1);
		assertThat(selectedGoodPlace).isNotNull();
		assertThat(selectedGoodPlace.getId()).isNotBlank();
		assertThat(selectedGoodPlace.getUserId()).isEqualTo(TEST_USER_ID);
		assertThat(selectedGoodPlace.getGroupId()).isEqualTo(groupId);
		assertThat(selectedGoodPlace.getPlaceId()).isEqualTo(PLACE_ID);
		assertThat(selectedGoodPlace.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("동일 그룹에 같은 장소를 중복 저장할 수 없다")
	void duplicateGoodPlaceInSameGroupFails() {
		String groupId = insertGroup(TEST_USER_ID, "친구 추천");
		GoodPlaceEntity goodPlace = createGoodPlace(TEST_USER_ID, groupId, PLACE_ID);
		goodPlaceMapper.insertGoodPlace(goodPlace);

		assertThatThrownBy(() -> goodPlaceMapper.insertGoodPlace(goodPlace))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("같은 장소를 서로 다른 그룹에 저장할 수 있다")
	void samePlaceCanBeSavedInDifferentGroups() {
		String firstGroupId = insertGroup(TEST_USER_ID, "친구 추천");
		String secondGroupId = insertGroup(TEST_USER_ID, "데이트");

		int firstInsertedCount = goodPlaceMapper.insertGoodPlace(
			createGoodPlace(TEST_USER_ID, firstGroupId, PLACE_ID));
		int secondInsertedCount = goodPlaceMapper.insertGoodPlace(
			createGoodPlace(TEST_USER_ID, secondGroupId, PLACE_ID));

		assertThat(firstInsertedCount).isEqualTo(1);
		assertThat(secondInsertedCount).isEqualTo(1);
		assertThat(goodPlaceMapper.selectGoodPlace(TEST_USER_ID, firstGroupId, PLACE_ID))
			.isNotNull();
		assertThat(goodPlaceMapper.selectGoodPlace(TEST_USER_ID, secondGroupId, PLACE_ID))
			.isNotNull();
	}

	@Test
	@DisplayName("그룹에 저장된 맛집과 장소 정보를 함께 조회한다")
	void selectGoodPlacesByGroup() {
		String groupId = insertGroup(TEST_USER_ID, "친구 추천");
		goodPlaceMapper.insertGoodPlace(createGoodPlace(TEST_USER_ID, groupId, PLACE_ID));
		goodPlaceMapper.insertGoodPlace(createGoodPlace(TEST_USER_ID, groupId, OTHER_PLACE_ID));

		List<GoodPlaceDetailResponse> goodPlaces =
			goodPlaceMapper.selectGoodPlacesByGroup(TEST_USER_ID, groupId, 0, 10);
		List<GoodPlaceDetailResponse> firstPage =
			goodPlaceMapper.selectGoodPlacesByGroup(TEST_USER_ID, groupId, 0, 1);
		List<GoodPlaceDetailResponse> secondPage =
			goodPlaceMapper.selectGoodPlacesByGroup(TEST_USER_ID, groupId, 1, 1);
		long totalElements =
			goodPlaceMapper.countGoodPlacesByGroup(TEST_USER_ID, groupId);

		assertThat(goodPlaces).hasSize(2);
		assertThat(firstPage).hasSize(1);
		assertThat(secondPage).hasSize(1);
		assertThat(firstPage.getFirst().getId())
			.isNotEqualTo(secondPage.getFirst().getId());
		assertThat(totalElements).isEqualTo(2);
		assertThat(goodPlaces)
			.extracting(goodPlace -> goodPlace.getPlace().getId())
			.containsExactlyInAnyOrder(PLACE_ID, OTHER_PLACE_ID);

		GoodPlaceDetailResponse response = goodPlaces.stream()
			.filter(goodPlace -> goodPlace.getPlace().getId().equals(PLACE_ID))
			.findFirst()
			.orElseThrow();

		assertThat(response.getId()).isNotBlank();
		assertThat(response.getGroupId()).isEqualTo(groupId);
		assertThat(response.getCreatedAt()).isNotNull();
		assertThat(response.getPlace().getName()).isEqualTo("테스트 맛집");
		assertThat(response.getPlace().getCategoryName()).isEqualTo("음식점");
		assertThat(response.getPlace().getRoadAddressName()).isEqualTo("서울시 테스트로 1");
		assertThat(response.getPlace().getX()).isEqualTo("127.000000");
		assertThat(response.getPlace().getY()).isEqualTo("37.000000");
	}

	@Test
	@DisplayName("지정한 사용자의 특정 그룹에서만 맛집을 삭제한다")
	void deleteGoodPlaceFromGroupChecksUserAndGroup() {
		String firstGroupId = insertGroup(TEST_USER_ID, "친구 추천");
		String secondGroupId = insertGroup(TEST_USER_ID, "데이트");
		goodPlaceMapper.insertGoodPlace(createGoodPlace(TEST_USER_ID, firstGroupId, PLACE_ID));
		goodPlaceMapper.insertGoodPlace(createGoodPlace(TEST_USER_ID, secondGroupId, PLACE_ID));

		int otherUserDeletedCount =
			goodPlaceMapper.deleteGoodPlaceFromGroup(OTHER_USER_ID, firstGroupId, PLACE_ID);
		int deletedCount =
			goodPlaceMapper.deleteGoodPlaceFromGroup(TEST_USER_ID, firstGroupId, PLACE_ID);

		assertThat(otherUserDeletedCount).isZero();
		assertThat(deletedCount).isEqualTo(1);
		assertThat(goodPlaceMapper.selectGoodPlace(TEST_USER_ID, firstGroupId, PLACE_ID))
			.isNull();
		assertThat(goodPlaceMapper.selectGoodPlace(TEST_USER_ID, secondGroupId, PLACE_ID))
			.isNotNull();
	}

	private String insertGroup(String userId, String name) {
		GroupEntity group = new GroupEntity();
		group.setUserId(userId);
		group.setName(name);
		groupMapper.insertGroup(group);
		return groupMapper.selectGroupByUserIdAndName(userId, name).getId();
	}

	private GoodPlaceEntity createGoodPlace(String userId, String groupId, String placeId) {
		GoodPlaceEntity goodPlace = new GoodPlaceEntity();
		goodPlace.setUserId(userId);
		goodPlace.setGroupId(groupId);
		goodPlace.setPlaceId(placeId);
		return goodPlace;
	}

	private void insertTestUser(String id, String email, String handle) {
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, email, password, handle, nickname)
			VALUES (?, ?, ?, ?, ?)
			""",
			id,
			email,
			"test-password",
			handle,
			"테스트 사용자"
		);
	}

	private void insertTestPlace(String id, String name) {
		jdbcTemplate.update(
			"""
			INSERT INTO places (
				id,
				name,
				category_name,
				road_address_name,
				x,
				y
			)
			VALUES (?, ?, ?, ?, ?, ?)
			""",
			id,
			name,
			"음식점",
			"서울시 테스트로 1",
			"127.000000",
			"37.000000"
		);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"DELETE FROM users WHERE id IN (?, ?)",
			TEST_USER_ID,
			OTHER_USER_ID
		);
		jdbcTemplate.update(
			"DELETE FROM places WHERE id IN (?, ?)",
			PLACE_ID,
			OTHER_PLACE_ID
		);
	}
}
