package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.gourming.model.dto.RecommendationDto.RecommendationCandidateRow;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("추천 후보 Mapper 테스트")
class RecommendationMapperTest {

	// 실제 DB의 다른 리뷰와 섞이지 않도록 존재하지 않는 버전 문자열을 쓴다.
	private static final String VERSION = "test-reco-v1";
	private static final String ME = "9a000000-0000-0000-0000-000000000001";
	private static final String OTHER = "9a000000-0000-0000-0000-000000000002";
	private static final String PLACE_ID = "test-place-reco-001";
	private static final String MY_REVIEW = "9b000000-0000-0000-0000-000000000001";
	private static final String LIKED_REVIEW = "9b000000-0000-0000-0000-000000000002";
	private static final String NO_VECTOR_REVIEW = "9b000000-0000-0000-0000-000000000003";
	private static final String OLD_VERSION_REVIEW = "9b000000-0000-0000-0000-000000000004";
	private static final String CANDIDATE_OLD = "9b000000-0000-0000-0000-000000000005";
	private static final String CANDIDATE_NEW = "9b000000-0000-0000-0000-000000000006";

	@Autowired
	private RecommendationMapper recommendationMapper;

	@Autowired
	private ReviewMapper reviewMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// JDBC로 데이터를 바꾼 뒤 같은 인자로 다시 조회할 때 MyBatis 1차 캐시를 비우기 위해 쓴다.
	@Autowired
	private SqlSession sqlSession;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertUser(ME, "reco-me@test.com", "@reco_me");
		insertUser(OTHER, "reco-other@test.com", "@reco_other");
		jdbcTemplate.update(
			"INSERT INTO places (id, name, category_name, category_group_code, road_address_name, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)",
			PLACE_ID, "Reco Place", "음식점 > 카페", "CE7", "Road", "127.0", "37.0");
		insertReview(MY_REVIEW, ME, LocalDateTime.of(2026, 9, 10, 0, 0));
		insertReview(LIKED_REVIEW, OTHER, LocalDateTime.of(2026, 9, 9, 0, 0));
		insertReview(NO_VECTOR_REVIEW, OTHER, LocalDateTime.of(2026, 9, 8, 0, 0));
		insertReview(OLD_VERSION_REVIEW, OTHER, LocalDateTime.of(2026, 9, 7, 0, 0));
		insertReview(CANDIDATE_OLD, OTHER, LocalDateTime.of(2026, 9, 1, 0, 0));
		insertReview(CANDIDATE_NEW, OTHER, LocalDateTime.of(2026, 9, 5, 0, 0));
		insertEmbedding(MY_REVIEW, VERSION);
		insertEmbedding(LIKED_REVIEW, VERSION);
		insertEmbedding(OLD_VERSION_REVIEW, "old-v0");
		insertEmbedding(CANDIDATE_OLD, VERSION);
		insertEmbedding(CANDIDATE_NEW, VERSION);
		jdbcTemplate.update("INSERT INTO likes (id, user_id, review_id) VALUES (UUID(), ?, ?)", ME, LIKED_REVIEW);
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("본인·좋아요·벡터 없음·버전 불일치 리뷰를 제외하고 최신순으로 후보를 조회한다")
	void selectCandidatesFilters() {
		List<RecommendationCandidateRow> candidates = recommendationMapper.selectCandidates(ME, VERSION, 500, null);

		assertThat(candidates).extracting(RecommendationCandidateRow::getReviewId)
			.containsExactly(CANDIDATE_NEW, CANDIDATE_OLD);
		assertThat(candidates.get(0).getPlaceId()).isEqualTo(PLACE_ID);
		assertThat(candidates.get(0).getEmbedding()).containsExactly(1f, 0f);
		assertThat(candidates.get(0).getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 5, 0, 0));
	}

	@Test
	@DisplayName("limit만큼만 조회된다")
	void selectCandidatesLimit() {
		List<RecommendationCandidateRow> candidates = recommendationMapper.selectCandidates(ME, VERSION, 1, null);

		assertThat(candidates).extracting(RecommendationCandidateRow::getReviewId).containsExactly(CANDIDATE_NEW);
	}

	@Test
	@DisplayName("selectReviewsByIds는 요청한 ID의 리뷰를 모두 반환한다")
	void selectReviewsByIds() {
		List<ReviewResponse> reviews = reviewMapper.selectReviewsByIds(List.of(CANDIDATE_OLD, CANDIDATE_NEW), ME);

		assertThat(reviews).extracting(ReviewResponse::getId).containsExactlyInAnyOrder(CANDIDATE_OLD, CANDIDATE_NEW);
		assertThat(reviews.get(0).getPlace().getId()).isEqualTo(PLACE_ID);
		assertThat(reviews.get(0).getAuthor().getId()).isEqualTo(OTHER);
	}

	@Test
	@DisplayName("asOf 이후 작성된 리뷰는 후보에 없고, asOf 이후 누른 좋아요는 제외에 쓰이지 않는다")
	void selectCandidatesRespectsAsOf() {
		LocalDateTime asOf = LocalDateTime.of(2026, 9, 3, 0, 0);
		// CANDIDATE_NEW(9/5 작성)는 asOf 이후라 빠지고, CANDIDATE_OLD(9/1 작성)만 남는다.
		assertThat(recommendationMapper.selectCandidates(ME, VERSION, 500, asOf))
			.extracting(RecommendationCandidateRow::getReviewId)
			.containsExactly(CANDIDATE_OLD);

		// asOf 이후에 누른 좋아요는 무시되어 CANDIDATE_OLD가 남는다.
		jdbcTemplate.update("INSERT INTO likes (id, user_id, review_id, created_at) VALUES (UUID(), ?, ?, ?)",
			ME, CANDIDATE_OLD, LocalDateTime.of(2026, 9, 4, 0, 0));
		sqlSession.clearCache();
		assertThat(recommendationMapper.selectCandidates(ME, VERSION, 500, asOf))
			.extracting(RecommendationCandidateRow::getReviewId)
			.containsExactly(CANDIDATE_OLD);

		// asOf 이전 좋아요면 제외된다.
		jdbcTemplate.update("UPDATE likes SET created_at = ? WHERE user_id = ? AND review_id = ?",
			LocalDateTime.of(2026, 9, 2, 0, 0), ME, CANDIDATE_OLD);
		sqlSession.clearCache();
		assertThat(recommendationMapper.selectCandidates(ME, VERSION, 500, asOf)).isEmpty();
	}

	private void insertUser(String id, String email, String handle) {
		jdbcTemplate.update("INSERT INTO users (id, email, password, handle, nickname) VALUES (?, ?, ?, ?, ?)",
			id, email, "pw", handle, "Reco User");
	}

	private void insertReview(String id, String userId, LocalDateTime at) {
		jdbcTemplate.update(
			"INSERT INTO reviews (id, content, images, rating_score, visited_at, place_id, user_id, created_at, updated_at) VALUES (?, ?, JSON_ARRAY(), ?, ?, ?, ?, ?, ?)",
			id, "리뷰", 4, LocalDate.of(2026, 9, 1), PLACE_ID, userId, at, at);
	}

	private void insertEmbedding(String reviewId, String version) {
		jdbcTemplate.update("INSERT INTO review_embeddings (review_id, embedding, embedder_version) VALUES (?, ?, ?)",
			reviewId, "[1.0, 0.0]", version);
	}

	private void deleteTestData() {
		jdbcTemplate.update("DELETE FROM likes WHERE user_id IN (?, ?)", ME, OTHER);
		jdbcTemplate.update("DELETE FROM reviews WHERE user_id IN (?, ?)", ME, OTHER);
		jdbcTemplate.update("DELETE FROM places WHERE id = ?", PLACE_ID);
		jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", ME, OTHER);
	}
}
