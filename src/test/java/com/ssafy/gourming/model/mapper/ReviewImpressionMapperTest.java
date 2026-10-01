package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("리뷰 노출 기록 Mapper 테스트")
class ReviewImpressionMapperTest {

	private static final String VIEWER = "9c000000-0000-0000-0000-000000000001";
	private static final String AUTHOR = "9c000000-0000-0000-0000-000000000002";
	private static final String PLACE_ID = "test-place-impression-001";
	private static final String REVIEW_A = "9d000000-0000-0000-0000-000000000001";
	private static final String REVIEW_B = "9d000000-0000-0000-0000-000000000002";
	private static final String OWN_REVIEW = "9d000000-0000-0000-0000-000000000003";
	private static final String MISSING = "9d000000-0000-0000-0000-00000000ffff";

	@Autowired
	private ReviewImpressionMapper reviewImpressionMapper;

	@Autowired
	private ReviewMapper reviewMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertUser(VIEWER, "impression-viewer@test.com", "@impression_viewer");
		insertUser(AUTHOR, "impression-author@test.com", "@impression_author");
		jdbcTemplate.update(
			"INSERT INTO places (id, name, category_name, category_group_code, road_address_name, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)",
			PLACE_ID, "Impression Place", "음식점 > 카페", "CE7", "Road", "127.0", "37.0");
		insertReview(REVIEW_A, AUTHOR);
		insertReview(REVIEW_B, AUTHOR);
		insertReview(OWN_REVIEW, VIEWER);
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("처음 본 리뷰는 seen_count 1로 저장된다")
	void upsertInsertsNewImpressions() {
		int changed = reviewImpressionMapper.upsertImpressions(VIEWER, List.of(REVIEW_A, REVIEW_B));

		assertThat(changed).isEqualTo(2);
		assertThat(seenCount(REVIEW_A)).isEqualTo(1);
		assertThat(seenCount(REVIEW_B)).isEqualTo(1);
	}

	@Test
	@DisplayName("같은 리뷰를 다시 기록하면 seen_count가 오르고 first_seen_at은 유지된다")
	void upsertIncrementsExistingImpression() {
		reviewImpressionMapper.upsertImpressions(VIEWER, List.of(REVIEW_A));
		LocalDateTime past = LocalDateTime.of(2026, 1, 1, 0, 0);
		jdbcTemplate.update(
			"UPDATE review_impressions SET first_seen_at = ?, last_seen_at = ? WHERE user_id = ? AND review_id = ?",
			past, past, VIEWER, REVIEW_A);

		reviewImpressionMapper.upsertImpressions(VIEWER, List.of(REVIEW_A));

		Map<String, Object> row = jdbcTemplate.queryForMap(
			"SELECT first_seen_at, last_seen_at, seen_count FROM review_impressions WHERE user_id = ? AND review_id = ?",
			VIEWER, REVIEW_A);
		assertThat(((Number) row.get("seen_count")).intValue()).isEqualTo(2);
		assertThat(row.get("first_seen_at").toString()).startsWith("2026-01-01");
		assertThat(row.get("last_seen_at").toString()).doesNotStartWith("2026-01-01");
	}

	@Test
	@DisplayName("존재하지 않는 리뷰와 본인 리뷰는 저장되지 않는다")
	void upsertIgnoresMissingAndOwnReviews() {
		int changed = reviewImpressionMapper.upsertImpressions(VIEWER, List.of(MISSING, OWN_REVIEW, REVIEW_A));

		assertThat(changed).isEqualTo(1);
		assertThat(countFor(MISSING)).isZero();
		assertThat(countFor(OWN_REVIEW)).isZero();
		assertThat(seenCount(REVIEW_A)).isEqualTo(1);
	}

	@Test
	@DisplayName("리뷰가 삭제되면 노출 기록도 cascade 삭제된다")
	void deleteReviewCascadesImpression() {
		reviewImpressionMapper.upsertImpressions(VIEWER, List.of(REVIEW_A));

		reviewMapper.deleteReview(REVIEW_A, AUTHOR);

		assertThat(countFor(REVIEW_A)).isZero();
	}

	private int seenCount(String reviewId) {
		return jdbcTemplate.queryForObject(
			"SELECT seen_count FROM review_impressions WHERE user_id = ? AND review_id = ?",
			Integer.class, VIEWER, reviewId);
	}

	private int countFor(String reviewId) {
		return jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM review_impressions WHERE user_id = ? AND review_id = ?",
			Integer.class, VIEWER, reviewId);
	}

	private void insertUser(String id, String email, String handle) {
		jdbcTemplate.update("INSERT INTO users (id, email, password, handle, nickname) VALUES (?, ?, ?, ?, ?)",
			id, email, "pw", handle, "Impression User");
	}

	private void insertReview(String id, String userId) {
		jdbcTemplate.update(
			"INSERT INTO reviews (id, content, images, rating_score, visited_at, place_id, user_id) VALUES (?, ?, JSON_ARRAY(), ?, ?, ?, ?)",
			id, "리뷰", 4, LocalDate.of(2026, 9, 1), PLACE_ID, userId);
	}

	private void deleteTestData() {
		jdbcTemplate.update("DELETE FROM reviews WHERE id IN (?, ?, ?)", REVIEW_A, REVIEW_B, OWN_REVIEW);
		jdbcTemplate.update("DELETE FROM places WHERE id = ?", PLACE_ID);
		jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", VIEWER, AUTHOR);
	}
}
