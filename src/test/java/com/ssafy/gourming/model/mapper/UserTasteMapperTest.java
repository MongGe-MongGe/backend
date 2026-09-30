package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteEvidenceRow;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("사용자 성향 행동 Mapper 테스트")
class UserTasteMapperTest {

	private static final String VERSION = "test-v1";
	private static final String USER_ID = "99000000-0000-0000-0000-000000000001";
	private static final String OTHER_USER_ID = "99000000-0000-0000-0000-000000000002";
	private static final String PLACE_ID = "test-place-taste-001";
	private static final String GROUP_A = "99100000-0000-0000-0000-000000000001";
	private static final String GROUP_B = "99100000-0000-0000-0000-000000000002";
	private static final String REVIEW_5 = "99200000-0000-0000-0000-000000000001";
	private static final String REVIEW_3 = "99200000-0000-0000-0000-000000000002";
	private static final String REVIEW_1 = "99200000-0000-0000-0000-000000000003";
	private static final String OTHER_REVIEW = "99200000-0000-0000-0000-000000000004";

	@Autowired
	private UserTasteMapper userTasteMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertUser(USER_ID, "taste-user@test.com", "@taste_user");
		insertUser(OTHER_USER_ID, "taste-other@test.com", "@taste_other");
		jdbcTemplate.update(
			"INSERT INTO places (id, name, category_name, category_group_code, road_address_name, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)",
			PLACE_ID, "Taste Place", "음식점 > 카페", "CE7", "Road", "127.0", "37.0");
		jdbcTemplate.update("INSERT INTO `groups` (id, user_id, name, default_group) VALUES (?, ?, ?, ?)",
			GROUP_A, USER_ID, "A", true);
		jdbcTemplate.update("INSERT INTO `groups` (id, user_id, name, default_group) VALUES (?, ?, ?, ?)",
			GROUP_B, USER_ID, "B", false);
		insertReview(REVIEW_5, USER_ID, 5);
		insertReview(REVIEW_3, USER_ID, 3);
		insertReview(REVIEW_1, USER_ID, 1);
		insertReview(OTHER_REVIEW, OTHER_USER_ID, 4);
		for (String reviewId : List.of(REVIEW_5, REVIEW_3, REVIEW_1, OTHER_REVIEW)) {
			jdbcTemplate.update(
				"INSERT INTO review_embeddings (review_id, embedding, embedder_version) VALUES (?, ?, ?)",
				reviewId, "[1.0, 0.0]", VERSION);
		}
		jdbcTemplate.update(
			"INSERT INTO place_embeddings (place_id, embedding, source, review_count, embedder_version) VALUES (?, ?, ?, ?, ?)",
			PLACE_ID, "[0.0, 1.0]", "REVIEWS", 1, VERSION);
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("같은 장소를 두 그룹에 저장해도 PLACE_SAVE 행은 하나이고 가중치는 5다")
	void selectEvidencesDistinctPlaceSave() {
		jdbcTemplate.update("INSERT INTO good_places (id, user_id, group_id, place_id) VALUES (UUID(), ?, ?, ?)",
			USER_ID, GROUP_A, PLACE_ID);
		jdbcTemplate.update("INSERT INTO good_places (id, user_id, group_id, place_id) VALUES (UUID(), ?, ?, ?)",
			USER_ID, GROUP_B, PLACE_ID);

		List<UserTasteEvidenceRow> rows = userTasteMapper.selectEvidences(USER_ID, VERSION, null);

		List<UserTasteEvidenceRow> saves = rows.stream()
			.filter(r -> r.getSource().equals(UserTasteEvidenceRow.SOURCE_PLACE_SAVE)).toList();
		assertThat(saves).hasSize(1);
		assertThat(saves.get(0).getWeight()).isEqualTo(5.0);
		assertThat(saves.get(0).getEmbedding()).containsExactly(0f, 1f);
		assertThat(saves.get(0).getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("좋아요 리뷰는 가중치 3이고 내 리뷰는 별점에 따라 4, -4이며 3점은 제외된다")
	void selectEvidencesWeights() {
		jdbcTemplate.update("INSERT INTO likes (id, user_id, review_id) VALUES (UUID(), ?, ?)", USER_ID, OTHER_REVIEW);

		List<UserTasteEvidenceRow> rows = userTasteMapper.selectEvidences(USER_ID, VERSION, null);

		assertThat(rows.stream().filter(r -> r.getSource().equals(UserTasteEvidenceRow.SOURCE_LIKE)))
			.hasSize(1)
			.allSatisfy(r -> assertThat(r.getWeight()).isEqualTo(3.0));
		List<Double> ownWeights = rows.stream()
			.filter(r -> r.getSource().equals(UserTasteEvidenceRow.SOURCE_OWN_REVIEW))
			.map(UserTasteEvidenceRow::getWeight).sorted().toList();
		assertThat(ownWeights).containsExactly(-4.0, 4.0);
	}

	@Test
	@DisplayName("현재 버전과 다른 벡터와 벡터 없는 행동은 조회되지 않는다")
	void selectEvidencesFiltersVersion() {
		jdbcTemplate.update("INSERT INTO likes (id, user_id, review_id) VALUES (UUID(), ?, ?)", USER_ID, OTHER_REVIEW);

		assertThat(userTasteMapper.selectEvidences(USER_ID, "other-version", null)).isEmpty();

		jdbcTemplate.update("DELETE FROM review_embeddings WHERE review_id = ?", OTHER_REVIEW);
		assertThat(userTasteMapper.selectEvidences(USER_ID, VERSION, null)
			.stream().filter(r -> r.getSource().equals(UserTasteEvidenceRow.SOURCE_LIKE))).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 행동은 조회되지 않는다")
	void selectEvidencesIsolatesUser() {
		List<UserTasteEvidenceRow> rows = userTasteMapper.selectEvidences(OTHER_USER_ID, VERSION, null);

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getSource()).isEqualTo(UserTasteEvidenceRow.SOURCE_OWN_REVIEW);
		assertThat(rows.get(0).getWeight()).isEqualTo(2.0);
	}

	@Test
	@DisplayName("asOf 이후에 생긴 저장·좋아요·내 리뷰는 조회되지 않는다")
	void selectEvidencesIgnoresActionsAfterAsOf() {
		LocalDateTime asOf = LocalDateTime.of(2026, 9, 22, 0, 0);
		jdbcTemplate.update(
			"INSERT INTO likes (id, user_id, review_id, created_at) VALUES (UUID(), ?, ?, ?)",
			USER_ID, OTHER_REVIEW, LocalDateTime.of(2026, 9, 20, 0, 0));
		jdbcTemplate.update(
			"INSERT INTO good_places (id, user_id, group_id, place_id, created_at) VALUES (UUID(), ?, ?, ?, ?)",
			USER_ID, GROUP_A, PLACE_ID, LocalDateTime.of(2026, 9, 25, 0, 0));
		jdbcTemplate.update("UPDATE reviews SET created_at = ? WHERE id = ?",
			LocalDateTime.of(2026, 9, 10, 0, 0), REVIEW_5);
		// REVIEW_1(별점 1)은 created_at이 현재 시각이라 asOf 이후다.

		List<UserTasteEvidenceRow> rows = userTasteMapper.selectEvidences(USER_ID, VERSION, asOf);

		assertThat(rows).extracting(UserTasteEvidenceRow::getSource)
			.containsExactlyInAnyOrder(UserTasteEvidenceRow.SOURCE_LIKE, UserTasteEvidenceRow.SOURCE_OWN_REVIEW);
		assertThat(rows.stream().filter(r -> r.getSource().equals(UserTasteEvidenceRow.SOURCE_OWN_REVIEW)))
			.allSatisfy(r -> assertThat(r.getWeight()).isEqualTo(4.0));

		assertThat(userTasteMapper.selectEvidences(USER_ID, VERSION, null))
			.extracting(UserTasteEvidenceRow::getSource)
			.contains(UserTasteEvidenceRow.SOURCE_PLACE_SAVE);
	}

	private void insertUser(String id, String email, String handle) {
		jdbcTemplate.update(
			"INSERT INTO users (id, email, password, handle, nickname) VALUES (?, ?, ?, ?, ?)",
			id, email, "pw", handle, "Taste User");
	}

	private void insertReview(String id, String userId, int rating) {
		jdbcTemplate.update(
			"INSERT INTO reviews (id, content, images, rating_score, visited_at, place_id, user_id) VALUES (?, ?, JSON_ARRAY(), ?, ?, ?, ?)",
			id, "리뷰 " + rating, rating, LocalDate.of(2026, 9, 1), PLACE_ID, userId);
	}

	private void deleteTestData() {
		jdbcTemplate.update("DELETE FROM likes WHERE user_id IN (?, ?)", USER_ID, OTHER_USER_ID);
		jdbcTemplate.update("DELETE FROM good_places WHERE user_id IN (?, ?)", USER_ID, OTHER_USER_ID);
		jdbcTemplate.update("DELETE FROM reviews WHERE id IN (?, ?, ?, ?)", REVIEW_5, REVIEW_3, REVIEW_1, OTHER_REVIEW);
		jdbcTemplate.update("DELETE FROM `groups` WHERE id IN (?, ?)", GROUP_A, GROUP_B);
		jdbcTemplate.update("DELETE FROM places WHERE id = ?", PLACE_ID);
		jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_ID, OTHER_USER_ID);
	}
}
