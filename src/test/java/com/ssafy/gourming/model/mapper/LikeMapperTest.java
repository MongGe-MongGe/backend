package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("좋아요 Mapper 테스트")
class LikeMapperTest {

	private static final String USER_ID = "95000000-0000-0000-0000-000000000001";
	private static final String OTHER_USER_ID = "95000000-0000-0000-0000-000000000002";
	private static final String PLACE_ID = "test-like-place-001";
	private static final String REVIEW_ID = "96000000-0000-0000-0000-000000000001";

	@Autowired
	private LikeMapper likeMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertTestUser(USER_ID, "like-user-1@test.com", "@like_user_1");
		insertTestUser(OTHER_USER_ID, "like-user-2@test.com", "@like_user_2");
		insertTestPlace();
		insertTestReview();
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("리뷰 좋아요를 저장하고 존재 여부와 개수를 조회한다")
	void insertLikeAndSelectStatus() {
		int insertedCount = likeMapper.insertLike(USER_ID, REVIEW_ID);

		assertThat(insertedCount).isEqualTo(1);
		assertThat(likeMapper.existsLike(USER_ID, REVIEW_ID)).isTrue();
		assertThat(likeMapper.countLikesByReview(REVIEW_ID)).isEqualTo(1);
	}

	@Test
	@DisplayName("같은 사용자는 같은 리뷰에 좋아요를 중복 저장하지 않는다")
	void duplicateLikeIsIgnored() {
		int firstInsertedCount = likeMapper.insertLike(USER_ID, REVIEW_ID);
		int duplicateInsertedCount = likeMapper.insertLike(USER_ID, REVIEW_ID);

		assertThat(firstInsertedCount).isEqualTo(1);
		assertThat(duplicateInsertedCount).isZero();
		assertThat(likeMapper.countLikesByReview(REVIEW_ID)).isEqualTo(1);
	}

	@Test
	@DisplayName("지정한 사용자의 리뷰 좋아요만 삭제한다")
	void deleteLikeChecksUserAndReview() {
		likeMapper.insertLike(USER_ID, REVIEW_ID);
		likeMapper.insertLike(OTHER_USER_ID, REVIEW_ID);

		int deletedCount = likeMapper.deleteLike(USER_ID, REVIEW_ID);

		assertThat(deletedCount).isEqualTo(1);
		assertThat(likeMapper.existsLike(USER_ID, REVIEW_ID)).isFalse();
		assertThat(likeMapper.existsLike(OTHER_USER_ID, REVIEW_ID)).isTrue();
		assertThat(likeMapper.countLikesByReview(REVIEW_ID)).isEqualTo(1);
	}

	@Test
	@DisplayName("존재하지 않는 리뷰 좋아요 삭제는 0건으로 처리한다")
	void deleteMissingLikeReturnsZero() {
		int deletedCount = likeMapper.deleteLike(USER_ID, REVIEW_ID);

		assertThat(deletedCount).isZero();
		assertThat(likeMapper.existsLike(USER_ID, REVIEW_ID)).isFalse();
		assertThat(likeMapper.countLikesByReview(REVIEW_ID)).isZero();
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
			"좋아요 테스트 사용자"
		);
	}

	private void insertTestPlace() {
		jdbcTemplate.update(
			"""
			INSERT INTO places (id, name, category_name, road_address_name, x, y)
			VALUES (?, ?, ?, ?, ?, ?)
			""",
			PLACE_ID,
			"좋아요 테스트 맛집",
			"FD6",
			"서울시 좋아요 테스트로 1",
			"127.000000",
			"37.000000"
		);
	}

	private void insertTestReview() {
		jdbcTemplate.update(
			"""
			INSERT INTO reviews (
				id,
				content,
				images,
				rating_score,
				visited_at,
				place_id,
				user_id
			)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""",
			REVIEW_ID,
			"like test review content",
			"[]",
			5,
			"2026-06-18",
			PLACE_ID,
			USER_ID
		);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"DELETE FROM likes WHERE review_id = ?",
			REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM reviews WHERE id = ?",
			REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM places WHERE id = ?",
			PLACE_ID
		);
		jdbcTemplate.update(
			"DELETE FROM users WHERE id IN (?, ?)",
			USER_ID,
			OTHER_USER_ID
		);
	}
}
