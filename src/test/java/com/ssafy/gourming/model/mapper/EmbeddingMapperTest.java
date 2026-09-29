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

import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingTargetRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.PlaceEmbeddingRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.ReviewEmbeddingRow;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("임베딩 Mapper 테스트")
class EmbeddingMapperTest {

	private static final String VERSION = "test-v1";
	private static final String USER_ID = "97000000-0000-0000-0000-000000000001";
	private static final String PLACE_ID = "test-place-embedding-001";
	private static final String EMPTY_PLACE_ID = "test-place-embedding-002";
	private static final String REVIEW_ID = "98000000-0000-0000-0000-000000000001";
	private static final String OTHER_REVIEW_ID = "98000000-0000-0000-0000-000000000002";

	@Autowired
	private EmbeddingMapper embeddingMapper;

	@Autowired
	private PlaceMapper placeMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		jdbcTemplate.update(
			"INSERT INTO users (id, email, password, handle, nickname) VALUES (?, ?, ?, ?, ?)",
			USER_ID, "embedding-user@test.com", "pw", "@embedding_user", "Embedding User");
		insertPlace(PLACE_ID, "Embedding Test Place");
		insertPlace(EMPTY_PLACE_ID, "Empty Place");
		insertReview(REVIEW_ID, "첫 리뷰", LocalDateTime.of(2026, 9, 1, 10, 0));
		insertReview(OTHER_REVIEW_ID, "둘째 리뷰", LocalDateTime.of(2026, 9, 2, 10, 0));
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("벡터가 없는 리뷰만 임베딩 대상으로 조회된다")
	void selectEmbeddingTargetsReturnsReviewsWithoutEmbedding() {
		upsert(REVIEW_ID, VERSION);

		List<EmbeddingTargetRow> targets = embeddingMapper.selectEmbeddingTargets(VERSION);

		assertThat(targets).extracting(EmbeddingTargetRow::getReviewId)
			.contains(OTHER_REVIEW_ID)
			.doesNotContain(REVIEW_ID);
		assertThat(targets.stream()
			.filter(t -> t.getReviewId().equals(OTHER_REVIEW_ID))
			.findFirst().get().getContent())
			.isEqualTo("둘째 리뷰");
	}

	@Test
	@DisplayName("벡터 생성 후 내용이 수정된 리뷰는 다시 대상이 된다")
	void selectEmbeddingTargetsReturnsUpdatedReview() {
		upsert(REVIEW_ID, VERSION);
		jdbcTemplate.update(
			"UPDATE review_embeddings SET updated_at = ? WHERE review_id = ?",
			LocalDateTime.of(2026, 8, 1, 0, 0), REVIEW_ID);

		List<EmbeddingTargetRow> targets = embeddingMapper.selectEmbeddingTargets(VERSION);

		assertThat(targets).extracting(EmbeddingTargetRow::getReviewId).contains(REVIEW_ID);
	}

	@Test
	@DisplayName("버전이 다른 벡터를 가진 리뷰는 대상이 된다")
	void selectEmbeddingTargetsReturnsVersionMismatch() {
		upsert(REVIEW_ID, "old-v0");

		List<EmbeddingTargetRow> targets = embeddingMapper.selectEmbeddingTargets(VERSION);

		assertThat(targets).extracting(EmbeddingTargetRow::getReviewId).contains(REVIEW_ID);
	}

	@Test
	@DisplayName("upsert 후 같은 reviewId로 다시 upsert하면 값이 갱신된다")
	void upsertReviewEmbeddingsUpdatesExisting() {
		upsert(REVIEW_ID, VERSION);
		ReviewEmbeddingRow updated = row(REVIEW_ID, new float[] {9f, 9f}, VERSION);
		embeddingMapper.upsertReviewEmbeddings(List.of(updated));

		List<ReviewEmbeddingRow> rows =
			embeddingMapper.selectReviewEmbeddingsByPlaceId(PLACE_ID, VERSION);

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getEmbedding()).containsExactly(9f, 9f);
		assertThat(rows.get(0).getEmbedderVersion()).isEqualTo(VERSION);
	}

	@Test
	@DisplayName("현재 버전 벡터가 있는 장소 ID만 조회된다")
	void selectPlaceIdsWithEmbeddedReviews() {
		upsert(REVIEW_ID, VERSION);

		assertThat(embeddingMapper.selectPlaceIdsWithEmbeddedReviews(VERSION)).contains(PLACE_ID);
		assertThat(embeddingMapper.selectPlaceIdsWithEmbeddedReviews("other")).doesNotContain(PLACE_ID);
	}

	@Test
	@DisplayName("리뷰가 없고 장소 벡터가 없는 장소가 카테고리 임베딩 대상이다")
	void selectPlaceIdsNeedingCategoryEmbedding() {
		List<String> before = embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION);
		assertThat(before).contains(EMPTY_PLACE_ID).doesNotContain(PLACE_ID);

		embeddingMapper.upsertPlaceEmbedding(
			placeRow(EMPTY_PLACE_ID, PlaceEmbeddingRow.SOURCE_CATEGORY, 0));

		List<String> after = embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION);
		assertThat(after).doesNotContain(EMPTY_PLACE_ID);
	}

	@Test
	@DisplayName("리뷰 삭제 시 리뷰 벡터가, 장소 삭제 시 장소 벡터가 cascade 삭제된다")
	void cascadeDelete() {
		upsert(REVIEW_ID, VERSION);
		embeddingMapper.upsertPlaceEmbedding(placeRow(PLACE_ID, PlaceEmbeddingRow.SOURCE_REVIEWS, 1));

		jdbcTemplate.update("DELETE FROM reviews WHERE id = ?", REVIEW_ID);
		assertThat(embeddingMapper.selectReviewEmbeddingsByPlaceId(PLACE_ID, VERSION)).isEmpty();

		placeMapper.deletePlaceById(PLACE_ID);
		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM place_embeddings WHERE place_id = ?", Integer.class, PLACE_ID);
		assertThat(count).isZero();
	}

	private void upsert(String reviewId, String version) {
		embeddingMapper.upsertReviewEmbeddings(List.of(row(reviewId, new float[] {1f, 2f}, version)));
	}

	private ReviewEmbeddingRow row(String reviewId, float[] embedding, String version) {
		ReviewEmbeddingRow row = new ReviewEmbeddingRow();
		row.setReviewId(reviewId);
		row.setEmbedding(embedding);
		row.setEmbedderVersion(version);
		return row;
	}

	private PlaceEmbeddingRow placeRow(String placeId, String source, int reviewCount) {
		PlaceEmbeddingRow row = new PlaceEmbeddingRow();
		row.setPlaceId(placeId);
		row.setEmbedding(new float[] {1f, 0f});
		row.setSource(source);
		row.setReviewCount(reviewCount);
		row.setEmbedderVersion(VERSION);
		return row;
	}

	private void insertPlace(String id, String name) {
		jdbcTemplate.update(
			"INSERT INTO places (id, name, category_name, category_group_code, road_address_name, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)",
			id, name, "음식점 > 카페 > 디저트카페", "CE7", "Seoul Embedding Road 1", "127.0", "37.0");
	}

	private void insertReview(String id, String content, LocalDateTime at) {
		jdbcTemplate.update(
			"INSERT INTO reviews (id, content, images, rating_score, visited_at, place_id, user_id, created_at, updated_at) VALUES (?, ?, JSON_ARRAY(), ?, ?, ?, ?, ?, ?)",
			id, content, 5, LocalDate.of(2026, 9, 1), PLACE_ID, USER_ID, at, at);
	}

	private void deleteTestData() {
		jdbcTemplate.update("DELETE FROM reviews WHERE id IN (?, ?)", REVIEW_ID, OTHER_REVIEW_ID);
		jdbcTemplate.update("DELETE FROM places WHERE id IN (?, ?)", PLACE_ID, EMPTY_PLACE_ID);
		jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
	}
}
