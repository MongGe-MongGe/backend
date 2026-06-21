package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.ssafy.gourming.model.dto.CommentDto.CommentEntity;
import com.ssafy.gourming.model.dto.CommentDto.CommentResponse;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("댓글 Mapper 테스트")
class CommentMapperTest {

	private static final String USER_ID = "97000000-0000-0000-0000-000000000001";
	private static final String OTHER_USER_ID = "97000000-0000-0000-0000-000000000002";
	private static final String PLACE_ID = "test-comment-place-001";
	private static final String REVIEW_ID = "98000000-0000-0000-0000-000000000001";
	private static final String COMMENT_ID = "99000000-0000-0000-0000-000000000001";
	private static final String OTHER_COMMENT_ID = "99000000-0000-0000-0000-000000000002";

	@Autowired
	private CommentMapper commentMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertTestUser(USER_ID, "comment-user-1@test.com", "@comment_user_1", "댓글 사용자 1");
		insertTestUser(OTHER_USER_ID, "comment-user-2@test.com", "@comment_user_2", "댓글 사용자 2");
		insertTestPlace();
		insertTestReview();
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("댓글을 저장하고 작성자 정보를 포함해 조회한다")
	void insertCommentAndSelectComment() {
		CommentEntity comment = createComment(COMMENT_ID, USER_ID, "댓글 내용");

		int insertedCount = commentMapper.insertComment(comment);
		CommentEntity entity = commentMapper.selectCommentEntityById(COMMENT_ID);
		CommentResponse response = commentMapper.selectCommentById(COMMENT_ID);

		assertThat(insertedCount).isEqualTo(1);
		assertThat(entity).isNotNull();
		assertThat(entity.getId()).isEqualTo(COMMENT_ID);
		assertThat(entity.getUserId()).isEqualTo(USER_ID);
		assertThat(entity.getReviewId()).isEqualTo(REVIEW_ID);
		assertThat(entity.getContent()).isEqualTo("댓글 내용");
		assertThat(entity.getCreatedAt()).isNotNull();
		assertThat(entity.getUpdatedAt()).isNotNull();
		assertThat(response.getAuthor()).isNotNull();
		assertThat(response.getAuthor().getId()).isEqualTo(USER_ID);
		assertThat(response.getAuthor().getNickname()).isEqualTo("댓글 사용자 1");
		assertThat(response.getAuthor().getHandle()).isEqualTo("@comment_user_1");
	}

	@Test
	@DisplayName("리뷰별 댓글 목록을 오래된 순서와 페이지네이션으로 조회한다")
	void selectCommentsByReview() {
		commentMapper.insertComment(createComment(COMMENT_ID, USER_ID, "첫 번째 댓글"));
		commentMapper.insertComment(createComment(OTHER_COMMENT_ID, OTHER_USER_ID, "두 번째 댓글"));

		List<CommentResponse> comments =
			commentMapper.selectCommentsByReview(REVIEW_ID, 0, 10);
		List<CommentResponse> firstPage =
			commentMapper.selectCommentsByReview(REVIEW_ID, 0, 1);
		List<CommentResponse> secondPage =
			commentMapper.selectCommentsByReview(REVIEW_ID, 1, 1);
		long count = commentMapper.countCommentsByReview(REVIEW_ID);

		assertThat(count).isEqualTo(2);
		assertThat(comments)
			.extracting(CommentResponse::getId)
			.containsExactly(COMMENT_ID, OTHER_COMMENT_ID);
		assertThat(firstPage)
			.extracting(CommentResponse::getId)
			.containsExactly(COMMENT_ID);
		assertThat(secondPage)
			.extracting(CommentResponse::getId)
			.containsExactly(OTHER_COMMENT_ID);
	}

	@Test
	@DisplayName("작성자 본인만 댓글을 수정하고 삭제할 수 있다")
	void updateAndDeleteCommentChecksOwner() {
		commentMapper.insertComment(createComment(COMMENT_ID, USER_ID, "수정 전 댓글"));

		CommentEntity otherUserUpdate = createComment(COMMENT_ID, OTHER_USER_ID, "다른 사용자 수정");
		int otherUserUpdatedCount = commentMapper.updateComment(otherUserUpdate);

		CommentEntity ownerUpdate = createComment(COMMENT_ID, USER_ID, "수정된 댓글");
		int ownerUpdatedCount = commentMapper.updateComment(ownerUpdate);
		CommentEntity updatedComment = commentMapper.selectCommentEntityById(COMMENT_ID);

		int otherUserDeletedCount = commentMapper.deleteComment(COMMENT_ID, OTHER_USER_ID);
		int ownerDeletedCount = commentMapper.deleteComment(COMMENT_ID, USER_ID);

		assertThat(otherUserUpdatedCount).isZero();
		assertThat(ownerUpdatedCount).isEqualTo(1);
		assertThat(updatedComment.getContent()).isEqualTo("수정된 댓글");
		assertThat(otherUserDeletedCount).isZero();
		assertThat(ownerDeletedCount).isEqualTo(1);
		assertThat(commentMapper.selectCommentEntityById(COMMENT_ID)).isNull();
	}

	private CommentEntity createComment(String id, String userId, String content) {
		CommentEntity comment = new CommentEntity();
		comment.setId(id);
		comment.setUserId(userId);
		comment.setReviewId(REVIEW_ID);
		comment.setContent(content);
		return comment;
	}

	private void insertTestUser(
		String id,
		String email,
		String handle,
		String nickname
	) {
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, email, password, handle, nickname)
			VALUES (?, ?, ?, ?, ?)
			""",
			id,
			email,
			"test-password",
			handle,
			nickname
		);
	}

	private void insertTestPlace() {
		jdbcTemplate.update(
			"""
			INSERT INTO places (id, name, category_name, road_address_name, x, y)
			VALUES (?, ?, ?, ?, ?, ?)
			""",
			PLACE_ID,
			"댓글 테스트 맛집",
			"FD6",
			"서울시 댓글 테스트로 1",
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
			"comment test review content",
			"[]",
			5,
			"2026-06-20",
			PLACE_ID,
			USER_ID
		);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"DELETE FROM comments WHERE review_id = ?",
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
