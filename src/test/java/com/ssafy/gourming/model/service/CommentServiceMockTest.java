package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.dto.CommentDto.CommentCreateRequest;
import com.ssafy.gourming.model.dto.CommentDto.CommentEntity;
import com.ssafy.gourming.model.dto.CommentDto.CommentPageResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentUpdateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.mapper.CommentMapper;
import com.ssafy.gourming.model.mapper.ReviewMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 서비스 Mock 단위 테스트")
class CommentServiceMockTest {

	private static final String USER_ID = "user-1";
	private static final String OTHER_USER_ID = "user-2";
	private static final String REVIEW_ID = "review-1";
	private static final String COMMENT_ID = "comment-1";

	@Mock
	private CommentMapper commentMapper;

	@Mock
	private ReviewMapper reviewMapper;

	@InjectMocks
	private CommentServiceImpl commentService;

	@Test
	@DisplayName("댓글을 작성하고 생성 결과를 반환한다")
	void createComment() {
		CommentCreateRequest request = createRequest(" 댓글 내용 ");
		CommentResponse response = createResponse();

		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());
		when(commentMapper.insertComment(any())).thenReturn(1);
		when(commentMapper.selectCommentById(any())).thenReturn(response);

		CommentResponse result = commentService.createComment(USER_ID, REVIEW_ID, request);

		assertThat(result).isSameAs(response);
		verify(commentMapper).insertComment(argThat(comment ->
			comment.getId() != null
				&& !comment.getId().isBlank()
				&& comment.getUserId().equals(USER_ID)
				&& comment.getReviewId().equals(REVIEW_ID)
				&& comment.getContent().equals("댓글 내용")
		));
	}

	@Test
	@DisplayName("존재하지 않는 리뷰에는 댓글을 작성할 수 없다")
	void createCommentOnMissingReviewFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(null);

		assertThatThrownBy(() ->
			commentService.createComment(USER_ID, REVIEW_ID, createRequest("댓글 내용")))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Review not found: " + REVIEW_ID);

		verify(commentMapper, never()).insertComment(any());
	}

	@Test
	@DisplayName("댓글 내용은 공백일 수 없다")
	void createCommentWithBlankContentFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());

		assertThatThrownBy(() ->
			commentService.createComment(USER_ID, REVIEW_ID, createRequest("   ")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Comment content is required");

		verify(commentMapper, never()).insertComment(any());
	}

	@Test
	@DisplayName("댓글 저장이 반영되지 않으면 생성에 실패한다")
	void createCommentDatabaseFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());
		when(commentMapper.insertComment(any())).thenReturn(0);

		assertThatThrownBy(() ->
			commentService.createComment(USER_ID, REVIEW_ID, createRequest("댓글 내용")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to create comment");
	}

	@Test
	@DisplayName("리뷰별 댓글 목록을 페이지 응답으로 반환한다")
	void getCommentsByReview() {
		List<CommentResponse> comments = List.of(createResponse());
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());
		when(commentMapper.countCommentsByReview(REVIEW_ID)).thenReturn(21L);
		when(commentMapper.selectCommentsByReview(REVIEW_ID, 20, 10)).thenReturn(comments);

		CommentPageResponse result = commentService.getCommentsByReview(REVIEW_ID, 2, 10);

		assertThat(result.getContent()).isSameAs(comments);
		assertThat(result.getPage()).isEqualTo(2);
		assertThat(result.getSize()).isEqualTo(10);
		assertThat(result.getTotalElements()).isEqualTo(21);
		assertThat(result.getTotalPages()).isEqualTo(3);
		assertThat(result.isFirst()).isFalse();
		assertThat(result.isLast()).isTrue();
		verify(commentMapper).selectCommentsByReview(REVIEW_ID, 20, 10);
	}

	@Test
	@DisplayName("존재하지 않는 리뷰의 댓글 목록은 조회할 수 없다")
	void getCommentsOfMissingReviewFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(null);

		assertThatThrownBy(() -> commentService.getCommentsByReview(REVIEW_ID, 0, 20))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Review not found: " + REVIEW_ID);

		verify(commentMapper, never()).selectCommentsByReview(any(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("댓글 목록 페이지 번호는 0 이상이어야 한다")
	void getCommentsWithNegativePageFails() {
		assertThatThrownBy(() -> commentService.getCommentsByReview(REVIEW_ID, -1, 20))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page must be zero or greater");

		verify(reviewMapper, never()).selectReviewEntityById(any());
		verify(commentMapper, never()).selectCommentsByReview(any(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("댓글 목록 페이지 크기는 1 이상 100 이하여야 한다")
	void getCommentsWithInvalidSizeFails() {
		assertThatThrownBy(() -> commentService.getCommentsByReview(REVIEW_ID, 0, 101))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Size must be between 1 and 100");

		verify(reviewMapper, never()).selectReviewEntityById(any());
		verify(commentMapper, never()).selectCommentsByReview(any(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("작성자 본인의 댓글을 수정한다")
	void updateComment() {
		CommentUpdateRequest request = createUpdateRequest(" 수정된 댓글 ");
		CommentEntity existingComment = createComment(USER_ID);
		CommentResponse response = createResponse();

		when(commentMapper.selectCommentEntityById(COMMENT_ID)).thenReturn(existingComment);
		when(commentMapper.updateComment(any())).thenReturn(1);
		when(commentMapper.selectCommentById(COMMENT_ID)).thenReturn(response);

		CommentResponse result = commentService.updateComment(USER_ID, COMMENT_ID, request);

		assertThat(result).isSameAs(response);
		verify(commentMapper).updateComment(argThat(comment ->
			comment.getId().equals(COMMENT_ID)
				&& comment.getUserId().equals(USER_ID)
				&& comment.getContent().equals("수정된 댓글")
		));
	}

	@Test
	@DisplayName("다른 사용자의 댓글은 수정할 수 없다")
	void updateOtherUsersCommentFails() {
		when(commentMapper.selectCommentEntityById(COMMENT_ID))
			.thenReturn(createComment(OTHER_USER_ID));

		assertThatThrownBy(() -> commentService.updateComment(
			USER_ID,
			COMMENT_ID,
			createUpdateRequest("수정된 댓글")
		))
			.isInstanceOf(SecurityException.class)
			.hasMessage("Comment does not belong to user");

		verify(commentMapper, never()).updateComment(any());
	}

	@Test
	@DisplayName("존재하지 않는 댓글은 수정할 수 없다")
	void updateMissingCommentFails() {
		when(commentMapper.selectCommentEntityById(COMMENT_ID)).thenReturn(null);

		assertThatThrownBy(() -> commentService.updateComment(
			USER_ID,
			COMMENT_ID,
			createUpdateRequest("수정된 댓글")
		))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Comment not found: " + COMMENT_ID);

		verify(commentMapper, never()).updateComment(any());
	}

	@Test
	@DisplayName("댓글 수정이 반영되지 않으면 실패한다")
	void updateCommentDatabaseFails() {
		when(commentMapper.selectCommentEntityById(COMMENT_ID))
			.thenReturn(createComment(USER_ID));
		when(commentMapper.updateComment(any())).thenReturn(0);

		assertThatThrownBy(() -> commentService.updateComment(
			USER_ID,
			COMMENT_ID,
			createUpdateRequest("수정된 댓글")
		))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to update comment: " + COMMENT_ID);
	}

	@Test
	@DisplayName("작성자 본인의 댓글을 삭제한다")
	void deleteComment() {
		when(commentMapper.selectCommentEntityById(COMMENT_ID))
			.thenReturn(createComment(USER_ID));
		when(commentMapper.deleteComment(COMMENT_ID, USER_ID)).thenReturn(1);

		commentService.deleteComment(USER_ID, COMMENT_ID);

		verify(commentMapper).deleteComment(COMMENT_ID, USER_ID);
	}

	@Test
	@DisplayName("다른 사용자의 댓글은 삭제할 수 없다")
	void deleteOtherUsersCommentFails() {
		when(commentMapper.selectCommentEntityById(COMMENT_ID))
			.thenReturn(createComment(OTHER_USER_ID));

		assertThatThrownBy(() -> commentService.deleteComment(USER_ID, COMMENT_ID))
			.isInstanceOf(SecurityException.class)
			.hasMessage("Comment does not belong to user");

		verify(commentMapper, never()).deleteComment(any(), any());
	}

	@Test
	@DisplayName("존재하지 않는 댓글은 삭제할 수 없다")
	void deleteMissingCommentFails() {
		when(commentMapper.selectCommentEntityById(COMMENT_ID)).thenReturn(null);

		assertThatThrownBy(() -> commentService.deleteComment(USER_ID, COMMENT_ID))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Comment not found: " + COMMENT_ID);

		verify(commentMapper, never()).deleteComment(any(), any());
	}

	@Test
	@DisplayName("댓글 삭제가 반영되지 않으면 실패한다")
	void deleteCommentDatabaseFails() {
		when(commentMapper.selectCommentEntityById(COMMENT_ID))
			.thenReturn(createComment(USER_ID));
		when(commentMapper.deleteComment(COMMENT_ID, USER_ID)).thenReturn(0);

		assertThatThrownBy(() -> commentService.deleteComment(USER_ID, COMMENT_ID))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to delete comment: " + COMMENT_ID);
	}

	private CommentCreateRequest createRequest(String content) {
		CommentCreateRequest request = new CommentCreateRequest();
		request.setContent(content);
		return request;
	}

	private CommentUpdateRequest createUpdateRequest(String content) {
		CommentUpdateRequest request = new CommentUpdateRequest();
		request.setContent(content);
		return request;
	}

	private ReviewEntity createReview() {
		ReviewEntity review = new ReviewEntity();
		review.setId(REVIEW_ID);
		return review;
	}

	private CommentEntity createComment(String userId) {
		CommentEntity comment = new CommentEntity();
		comment.setId(COMMENT_ID);
		comment.setUserId(userId);
		comment.setReviewId(REVIEW_ID);
		comment.setContent("기존 댓글");
		return comment;
	}

	private CommentResponse createResponse() {
		CommentResponse response = new CommentResponse();
		response.setId(COMMENT_ID);
		response.setReviewId(REVIEW_ID);
		response.setContent("댓글 내용");
		return response;
	}
}
