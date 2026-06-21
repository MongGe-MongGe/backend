package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.dto.CommentDto.AuthorSummary;
import com.ssafy.gourming.model.dto.CommentDto.CommentCreateRequest;
import com.ssafy.gourming.model.dto.CommentDto.CommentPageResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentResponse;
import com.ssafy.gourming.model.dto.CommentDto.CommentUpdateRequest;
import com.ssafy.gourming.model.service.CommentService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@DisplayName("댓글 컨트롤러 테스트")
class CommentControllerTest {

	private static final String USER_ID = "user-1";
	private static final String REVIEW_ID = "review-1";
	private static final String COMMENT_ID = "comment-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CommentService commentService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("인증된 사용자가 댓글을 작성한다")
	void createComment() throws Exception {
		when(commentService.createComment(eq(USER_ID), eq(REVIEW_ID), any()))
			.thenReturn(createResponse("댓글 내용"));

		mockMvc.perform(post("/api/reviews/{reviewId}/comments", REVIEW_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest("댓글 내용"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(COMMENT_ID))
			.andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
			.andExpect(jsonPath("$.content").value("댓글 내용"))
			.andExpect(jsonPath("$.author.id").value(USER_ID));

		verify(commentService).createComment(
			eq(USER_ID),
			eq(REVIEW_ID),
			org.mockito.ArgumentMatchers.argThat(request ->
				"댓글 내용".equals(request.getContent()))
		);
	}

	@Test
	@DisplayName("인증 없이 댓글을 작성할 수 없다")
	void createCommentWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(post("/api/reviews/{reviewId}/comments", REVIEW_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest("댓글 내용"))))
			.andExpect(status().isUnauthorized());

		verify(commentService, never()).createComment(any(), any(), any());
	}

	@Test
	@DisplayName("내용이 공백인 댓글 작성 요청을 거절한다")
	void createBlankCommentFails() throws Exception {
		mockMvc.perform(post("/api/reviews/{reviewId}/comments", REVIEW_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest("   "))))
			.andExpect(status().isBadRequest());

		verify(commentService, never()).createComment(any(), any(), any());
	}

	@Test
	@DisplayName("인증 없이 리뷰의 댓글 목록을 기본 페이지로 조회한다")
	void getCommentsByReview() throws Exception {
		when(commentService.getCommentsByReview(REVIEW_ID, 0, 20))
			.thenReturn(createPageResponse());

		mockMvc.perform(get("/api/reviews/{reviewId}/comments", REVIEW_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(COMMENT_ID))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1));

		verify(commentService).getCommentsByReview(REVIEW_ID, 0, 20);
	}

	@Test
	@DisplayName("잘못된 댓글 페이지 요청을 거절한다")
	void getCommentsWithInvalidPageFails() throws Exception {
		mockMvc.perform(get("/api/reviews/{reviewId}/comments", REVIEW_ID)
				.param("page", "-1")
				.param("size", "101"))
			.andExpect(status().isBadRequest());

		verify(commentService, never()).getCommentsByReview(any(), anyInt(), anyInt());
	}

	@Test
	@DisplayName("인증된 사용자가 댓글을 수정한다")
	void updateComment() throws Exception {
		when(commentService.updateComment(eq(USER_ID), eq(COMMENT_ID), any()))
			.thenReturn(createResponse("수정된 댓글"));

		mockMvc.perform(put("/api/comments/{commentId}", COMMENT_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest("수정된 댓글"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(COMMENT_ID))
			.andExpect(jsonPath("$.content").value("수정된 댓글"));

		verify(commentService).updateComment(
			eq(USER_ID),
			eq(COMMENT_ID),
			org.mockito.ArgumentMatchers.argThat(request ->
				"수정된 댓글".equals(request.getContent()))
		);
	}

	@Test
	@DisplayName("인증 없이 댓글을 수정할 수 없다")
	void updateCommentWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(put("/api/comments/{commentId}", COMMENT_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest("수정된 댓글"))))
			.andExpect(status().isUnauthorized());

		verify(commentService, never()).updateComment(any(), any(), any());
	}

	@Test
	@DisplayName("인증된 사용자가 댓글을 삭제한다")
	void deleteComment() throws Exception {
		mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID)
				.with(authentication(loginAuthentication())))
			.andExpect(status().isNoContent());

		verify(commentService).deleteComment(USER_ID, COMMENT_ID);
	}

	@Test
	@DisplayName("인증 없이 댓글을 삭제할 수 없다")
	void deleteCommentWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID))
			.andExpect(status().isUnauthorized());

		verify(commentService, never()).deleteComment(any(), any());
	}

	private Authentication loginAuthentication() {
		return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
	}

	private CommentCreateRequest createRequest(String content) {
		CommentCreateRequest request = new CommentCreateRequest();
		request.setContent(content);
		return request;
	}

	private CommentUpdateRequest updateRequest(String content) {
		CommentUpdateRequest request = new CommentUpdateRequest();
		request.setContent(content);
		return request;
	}

	private CommentResponse createResponse(String content) {
		AuthorSummary author = new AuthorSummary();
		author.setId(USER_ID);
		author.setNickname("댓글 사용자");
		author.setHandle("@comment_user");
		author.setProfileImage("/images/profile.png");

		CommentResponse response = new CommentResponse();
		response.setId(COMMENT_ID);
		response.setReviewId(REVIEW_ID);
		response.setContent(content);
		response.setAuthor(author);
		response.setCreatedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
		response.setUpdatedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
		return response;
	}

	private CommentPageResponse createPageResponse() {
		CommentPageResponse response = new CommentPageResponse();
		response.setContent(List.of(createResponse("댓글 내용")));
		response.setPage(0);
		response.setSize(20);
		response.setTotalElements(1);
		response.setTotalPages(1);
		response.setFirst(true);
		response.setLast(true);
		return response;
	}
}
