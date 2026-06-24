package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.ssafy.gourming.model.dto.PopularFeedDto.PopularReviewScoreRow;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("인기피드 Mapper 테스트")
class PopularFeedMapperTest {

	private static final int WINDOW_DAYS = 99;
	private static final String AUTHOR_ID = "9a000000-0000-0000-0000-000000000001";
	private static final String LIKE_USER_ID = "9a000000-0000-0000-0000-000000000002";
	private static final String COMMENT_USER_ID = "9a000000-0000-0000-0000-000000000003";
	private static final String PLACE_ID = "test-popular-place-001";
	private static final String POPULAR_REVIEW_ID = "9b000000-0000-0000-0000-000000000001";
	private static final String RECENT_REVIEW_ID = "9b000000-0000-0000-0000-000000000002";
	private static final String OLD_REVIEW_ID = "9b000000-0000-0000-0000-000000000003";

	@Autowired
	private PopularFeedMapper popularFeedMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		createPopularReviewScoresTableIfNeeded();
		deleteTestData();
		insertTestUser(AUTHOR_ID, "popular-author@test.com", "@popular_author");
		insertTestUser(LIKE_USER_ID, "popular-like@test.com", "@popular_like");
		insertTestUser(COMMENT_USER_ID, "popular-comment@test.com", "@popular_comment");
		insertTestPlace();
		insertTestReview(POPULAR_REVIEW_ID, "DATE_SUB(NOW(), INTERVAL 1 DAY)");
		insertTestReview(RECENT_REVIEW_ID, "NOW()");
		insertTestReview(OLD_REVIEW_ID, "DATE_SUB(NOW(), INTERVAL 100 DAY)");
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("최근 window 안의 리뷰별 인기 점수를 계산하고 점수순으로 정렬한다")
	void calculatePopularScores() {
		insertLike("9c000000-0000-0000-0000-000000000001", LIKE_USER_ID, POPULAR_REVIEW_ID);
		insertLike("9c000000-0000-0000-0000-000000000002", COMMENT_USER_ID, POPULAR_REVIEW_ID);
		insertComment("9d000000-0000-0000-0000-000000000001", COMMENT_USER_ID, POPULAR_REVIEW_ID);
		insertLike("9c000000-0000-0000-0000-000000000003", LIKE_USER_ID, OLD_REVIEW_ID);
		insertComment("9d000000-0000-0000-0000-000000000002", COMMENT_USER_ID, OLD_REVIEW_ID);

		List<PopularReviewScoreRow> scores = popularFeedMapper.calculatePopularScores(WINDOW_DAYS);
		List<PopularReviewScoreRow> testScores = scores.stream()
			.filter(score ->
				POPULAR_REVIEW_ID.equals(score.getReviewId())
					|| RECENT_REVIEW_ID.equals(score.getReviewId())
					|| OLD_REVIEW_ID.equals(score.getReviewId())
			)
			.toList();

		assertThat(testScores).hasSize(2);
		assertThat(testScores)
			.extracting(PopularReviewScoreRow::getReviewId)
			.containsExactly(POPULAR_REVIEW_ID, RECENT_REVIEW_ID);

		PopularReviewScoreRow popularReviewScore = testScores.get(0);
		assertThat(popularReviewScore.getLikeCount()).isEqualTo(2);
		assertThat(popularReviewScore.getCommentCount()).isEqualTo(1);
		assertThat(popularReviewScore.getScore()).isGreaterThan(testScores.get(1).getScore());
		assertThat(popularReviewScore.getWindowDays()).isEqualTo(WINDOW_DAYS);
		assertThat(popularReviewScore.getCalculatedAt()).isNotNull();
	}

	@Test
	@DisplayName("집계 결과를 일괄 저장하고 window 단위로 삭제한다")
	void insertAndDeletePopularScores() {
		PopularReviewScoreRow firstScore = createScoreRow(POPULAR_REVIEW_ID, 10.5, 1);
		PopularReviewScoreRow secondScore = createScoreRow(RECENT_REVIEW_ID, 8.0, 2);

		int insertedCount = popularFeedMapper.insertPopularScores(List.of(firstScore, secondScore));
		Integer savedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM popular_review_scores WHERE window_days = ?",
			Integer.class,
			WINDOW_DAYS
		);

		popularFeedMapper.deleteScoresByWindowDays(WINDOW_DAYS);
		Integer remainingCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM popular_review_scores WHERE window_days = ?",
			Integer.class,
			WINDOW_DAYS
		);

		assertThat(insertedCount).isEqualTo(2);
		assertThat(savedCount).isEqualTo(2);
		assertThat(remainingCount).isZero();
	}

	private PopularReviewScoreRow createScoreRow(String reviewId, double score, int rankNo) {
		PopularReviewScoreRow row = new PopularReviewScoreRow();
		row.setReviewId(reviewId);
		row.setScore(score);
		row.setLikeCount(rankNo);
		row.setCommentCount(0);
		row.setRankNo(rankNo);
		row.setWindowDays(WINDOW_DAYS);
		row.setCalculatedAt(LocalDateTime.now());
		return row;
	}

	private void createPopularReviewScoresTableIfNeeded() {
		jdbcTemplate.execute(
			"""
			CREATE TABLE IF NOT EXISTS popular_review_scores (
				review_id      CHAR(36)       NOT NULL,
				score          DECIMAL(10, 4) NOT NULL,
				like_count     BIGINT         NOT NULL DEFAULT 0,
				comment_count  BIGINT         NOT NULL DEFAULT 0,
				rank_no        INT            NOT NULL,
				window_days    INT            NOT NULL,
				calculated_at  DATETIME       NOT NULL,

				PRIMARY KEY (window_days, review_id),
				UNIQUE KEY uq_popular_review_rank (window_days, rank_no),
				KEY idx_popular_review_score (window_days, score DESC),
				CONSTRAINT fk_popular_review_scores_review
					FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
			)
			"""
		);
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
			"인기피드 테스트 사용자"
		);
	}

	private void insertTestPlace() {
		jdbcTemplate.update(
			"""
			INSERT INTO places (
				id, name, category_name, category_group_code, road_address_name, x, y
			)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""",
			PLACE_ID,
			"인기피드 테스트 맛집",
			"음식점 > 한식",
			"FD6",
			"서울시 인기피드 테스트로 1",
			"127.000000",
			"37.000000"
		);
	}

	private void insertTestReview(String id, String createdAtExpression) {
		jdbcTemplate.update(
			"""
			INSERT INTO reviews (
				id,
				content,
				images,
				rating_score,
				visited_at,
				place_id,
				user_id,
				created_at
			)
			VALUES (?, ?, ?, ?, ?, ?, ?, %s)
			""".formatted(createdAtExpression),
			id,
			"popular feed test review content",
			"[]",
			5,
			"2026-06-18",
			PLACE_ID,
			AUTHOR_ID
		);
	}

	private void insertLike(String id, String userId, String reviewId) {
		jdbcTemplate.update(
			"""
			INSERT INTO likes (id, user_id, review_id)
			VALUES (?, ?, ?)
			""",
			id,
			userId,
			reviewId
		);
	}

	private void insertComment(String id, String userId, String reviewId) {
		jdbcTemplate.update(
			"""
			INSERT INTO comments (id, user_id, review_id, content)
			VALUES (?, ?, ?, ?)
			""",
			id,
			userId,
			reviewId,
			"popular feed test comment"
		);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"DELETE FROM popular_review_scores WHERE review_id IN (?, ?, ?)",
			POPULAR_REVIEW_ID,
			RECENT_REVIEW_ID,
			OLD_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM likes WHERE review_id IN (?, ?, ?)",
			POPULAR_REVIEW_ID,
			RECENT_REVIEW_ID,
			OLD_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM comments WHERE review_id IN (?, ?, ?)",
			POPULAR_REVIEW_ID,
			RECENT_REVIEW_ID,
			OLD_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM reviews WHERE id IN (?, ?, ?)",
			POPULAR_REVIEW_ID,
			RECENT_REVIEW_ID,
			OLD_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM places WHERE id = ?",
			PLACE_ID
		);
		jdbcTemplate.update(
			"DELETE FROM users WHERE id IN (?, ?, ?)",
			AUTHOR_ID,
			LIKE_USER_ID,
			COMMENT_USER_ID
		);
	}
}
