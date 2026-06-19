package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.dto.LikeDto.LikeResponse;
import com.ssafy.gourming.model.service.LikeService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(LikeController.class)
@Import(SecurityConfig.class)
@DisplayName("좋아요 컨트롤러 테스트")
class LikeControllerTest {

	private static final String USER_ID = "user-1";
	private static final String REVIEW_ID = "review-1";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LikeService likeService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("인증된 사용자가 리뷰 좋아요를 등록한다")
	void likeReview() throws Exception {
		when(likeService.likeReview(USER_ID, REVIEW_ID))
			.thenReturn(createResponse(true, 3));

		mockMvc.perform(post("/api/reviews/{reviewId}/likes", REVIEW_ID)
				.with(authentication(loginAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
			.andExpect(jsonPath("$.likedByMe").value(true))
			.andExpect(jsonPath("$.likeCount").value(3));

		verify(likeService).likeReview(USER_ID, REVIEW_ID);
	}

	@Test
	@DisplayName("인증 없이 리뷰 좋아요를 등록할 수 없다")
	void likeReviewWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(post("/api/reviews/{reviewId}/likes", REVIEW_ID))
			.andExpect(status().isUnauthorized());

		verify(likeService, never()).likeReview(any(), any());
	}

	@Test
	@DisplayName("인증된 사용자가 리뷰 좋아요를 취소한다")
	void unlikeReview() throws Exception {
		when(likeService.unlikeReview(USER_ID, REVIEW_ID))
			.thenReturn(createResponse(false, 2));

		mockMvc.perform(delete("/api/reviews/{reviewId}/likes", REVIEW_ID)
				.with(authentication(loginAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
			.andExpect(jsonPath("$.likedByMe").value(false))
			.andExpect(jsonPath("$.likeCount").value(2));

		verify(likeService).unlikeReview(USER_ID, REVIEW_ID);
	}

	@Test
	@DisplayName("인증 없이 리뷰 좋아요를 취소할 수 없다")
	void unlikeReviewWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(delete("/api/reviews/{reviewId}/likes", REVIEW_ID))
			.andExpect(status().isUnauthorized());

		verify(likeService, never()).unlikeReview(any(), any());
	}

	private Authentication loginAuthentication() {
		return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
	}

	private LikeResponse createResponse(boolean likedByMe, long likeCount) {
		LikeResponse response = new LikeResponse();
		response.setReviewId(REVIEW_ID);
		response.setLikedByMe(likedByMe);
		response.setLikeCount(likeCount);
		return response;
	}
}
