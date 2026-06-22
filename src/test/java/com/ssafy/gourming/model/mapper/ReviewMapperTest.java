package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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

import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("리뷰 Mapper 테스트")
class ReviewMapperTest {

	private static final String USER_ID = "91000000-0000-0000-0000-000000000001";
	private static final String OTHER_USER_ID = "91000000-0000-0000-0000-000000000002";
	private static final String PLACE_ID = "test-review-place-001";
	private static final String OTHER_PLACE_ID = "test-review-place-002";
	private static final String REVIEW_ID = "92000000-0000-0000-0000-000000000001";
	private static final String OTHER_REVIEW_ID = "92000000-0000-0000-0000-000000000002";

	@Autowired
	private ReviewMapper reviewMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertTestUser(USER_ID, "review-user-1@test.com", "@review_user_1");
		insertTestUser(OTHER_USER_ID, "review-user-2@test.com", "@review_user_2");
		insertTestPlace(PLACE_ID, "Review Test Place");
		insertTestPlace(OTHER_PLACE_ID, "Other Review Test Place");
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("리뷰를 저장하고 이미지 목록을 포함한 엔티티로 조회한다")
	void insertReviewAndSelectReviewEntityById() {
		ReviewEntity review = createReview(REVIEW_ID, USER_ID, PLACE_ID);

		int insertedCount = reviewMapper.insertReview(review);
		ReviewEntity selectedReview = reviewMapper.selectReviewEntityById(REVIEW_ID);

		assertThat(insertedCount).isEqualTo(1);
		assertThat(selectedReview).isNotNull();
		assertThat(selectedReview.getId()).isEqualTo(REVIEW_ID);
		assertThat(selectedReview.getContent()).isEqualTo("review content");
		assertThat(selectedReview.getImages()).containsExactly("/images/review-1.png", "/images/review-2.png");
		assertThat(selectedReview.getRatingScore()).isEqualTo(5);
		assertThat(selectedReview.getVisitedAt()).isEqualTo(LocalDate.of(2026, 6, 18));
		assertThat(selectedReview.getPlaceId()).isEqualTo(PLACE_ID);
		assertThat(selectedReview.getUserId()).isEqualTo(USER_ID);
		assertThat(selectedReview.getCreatedAt()).isNotNull();
		assertThat(selectedReview.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("리뷰 상세에 장소, 작성자, 좋아요 수, 댓글 수를 포함해 조회한다")
	void selectReviewById() {
		reviewMapper.insertReview(createReview(REVIEW_ID, USER_ID, PLACE_ID));
		insertLike("93000000-0000-0000-0000-000000000001", OTHER_USER_ID, REVIEW_ID);
		insertComment("94000000-0000-0000-0000-000000000001", OTHER_USER_ID, REVIEW_ID);

		ReviewResponse response = reviewMapper.selectReviewById(REVIEW_ID, OTHER_USER_ID);

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(REVIEW_ID);
		assertThat(response.getImages()).containsExactly("/images/review-1.png", "/images/review-2.png");
		assertThat(response.getPlace()).isNotNull();
		assertThat(response.getPlace().getId()).isEqualTo(PLACE_ID);
		assertThat(response.getPlace().getName()).isEqualTo("Review Test Place");
		assertThat(response.getPlace().getCategoryName()).isEqualTo("음식점 > 한식");
		assertThat(response.getPlace().getCategoryGroupCode()).isEqualTo("FD6");
		assertThat(response.getAuthor()).isNotNull();
		assertThat(response.getAuthor().getId()).isEqualTo(USER_ID);
		assertThat(response.getAuthor().getHandle()).isEqualTo("@review_user_1");
		assertThat(response.getLikeCount()).isEqualTo(1);
		assertThat(response.getCommentCount()).isEqualTo(1);
		assertThat(response.isLikedByMe()).isTrue();
	}

	@Test
	@DisplayName("장소별 리뷰 목록을 페이지네이션으로 조회한다")
	void selectReviewsByPlace() {
		reviewMapper.insertReview(createReview(REVIEW_ID, USER_ID, PLACE_ID));
		reviewMapper.insertReview(createReview(OTHER_REVIEW_ID, OTHER_USER_ID, PLACE_ID));
		insertLike("93000000-0000-0000-0000-000000000001", OTHER_USER_ID, REVIEW_ID);

		List<ReviewResponse> reviews = reviewMapper.selectReviewsByPlace(PLACE_ID, OTHER_USER_ID, 0, 10);
		long count = reviewMapper.countReviewsByPlace(PLACE_ID);

		assertThat(count).isEqualTo(2);
		assertThat(reviews).hasSize(2);
		assertThat(reviews)
			.extracting(ReviewResponse::getId)
			.containsExactly(OTHER_REVIEW_ID, REVIEW_ID);
		assertThat(reviews.get(0).isLikedByMe()).isFalse();
		assertThat(reviews.get(1).isLikedByMe()).isTrue();
	}

	@Test
	@DisplayName("사용자별 리뷰 목록을 페이지네이션으로 조회한다")
	void selectReviewsByUser() {
		reviewMapper.insertReview(createReview(REVIEW_ID, USER_ID, PLACE_ID));
		reviewMapper.insertReview(createReview(OTHER_REVIEW_ID, USER_ID, OTHER_PLACE_ID));

		List<ReviewResponse> reviews = reviewMapper.selectReviewsByUser(USER_ID, null, 0, 10);
		long count = reviewMapper.countReviewsByUser(USER_ID);

		assertThat(count).isEqualTo(2);
		assertThat(reviews).hasSize(2);
		assertThat(reviews)
			.extracting(ReviewResponse::getId)
			.containsExactly(OTHER_REVIEW_ID, REVIEW_ID);
		assertThat(reviews)
			.extracting(ReviewResponse::isLikedByMe)
			.containsExactly(false, false);
	}

	@Test
	@DisplayName("작성자 본인만 리뷰를 수정하고 삭제할 수 있다")
	void updateAndDeleteReviewChecksOwner() {
		reviewMapper.insertReview(createReview(REVIEW_ID, USER_ID, PLACE_ID));

		ReviewEntity otherUserUpdate = createReview(REVIEW_ID, OTHER_USER_ID, PLACE_ID);
		otherUserUpdate.setContent("other user update");
		int otherUserUpdatedCount = reviewMapper.updateReview(otherUserUpdate);

		ReviewEntity ownerUpdate = createReview(REVIEW_ID, USER_ID, PLACE_ID);
		ownerUpdate.setContent("updated review content");
		ownerUpdate.setImages(List.of("/images/updated-review.png"));
		ownerUpdate.setRatingScore(4);
		int ownerUpdatedCount = reviewMapper.updateReview(ownerUpdate);
		ReviewEntity updatedReview = reviewMapper.selectReviewEntityById(REVIEW_ID);

		int otherUserDeletedCount = reviewMapper.deleteReview(REVIEW_ID, OTHER_USER_ID);
		int ownerDeletedCount = reviewMapper.deleteReview(REVIEW_ID, USER_ID);

		assertThat(otherUserUpdatedCount).isZero();
		assertThat(ownerUpdatedCount).isEqualTo(1);
		assertThat(updatedReview.getContent()).isEqualTo("updated review content");
		assertThat(updatedReview.getImages()).containsExactly("/images/updated-review.png");
		assertThat(updatedReview.getRatingScore()).isEqualTo(4);
		assertThat(otherUserDeletedCount).isZero();
		assertThat(ownerDeletedCount).isEqualTo(1);
		assertThat(reviewMapper.selectReviewEntityById(REVIEW_ID)).isNull();
	}

	private ReviewEntity createReview(String id, String userId, String placeId) {
		ReviewEntity review = new ReviewEntity();
		review.setId(id);
		review.setUserId(userId);
		review.setPlaceId(placeId);
		review.setContent("review content");
		review.setImages(List.of("/images/review-1.png", "/images/review-2.png"));
		review.setRatingScore(5);
		review.setVisitedAt(LocalDate.of(2026, 6, 18));
		return review;
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
			"Review Test User"
		);
	}

	private void insertTestPlace(String id, String name) {
		jdbcTemplate.update(
			"""
			INSERT INTO places (
				id, name, category_name, category_group_code, road_address_name, x, y
			)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""",
			id,
			name,
			"음식점 > 한식",
			"FD6",
			"Seoul Review Test Road 1",
			"127.000000",
			"37.000000"
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
			"review comment"
		);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"DELETE FROM likes WHERE review_id IN (?, ?)",
			REVIEW_ID,
			OTHER_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM comments WHERE review_id IN (?, ?)",
			REVIEW_ID,
			OTHER_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM reviews WHERE id IN (?, ?)",
			REVIEW_ID,
			OTHER_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM places WHERE id IN (?, ?)",
			PLACE_ID,
			OTHER_PLACE_ID
		);
		jdbcTemplate.update(
			"DELETE FROM users WHERE id IN (?, ?)",
			USER_ID,
			OTHER_USER_ID
		);
	}
}
