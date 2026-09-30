package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.dto.RecommendationDto.RecommendationPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.service.RecommendationService;
import com.ssafy.gourming.model.service.ReviewService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@DisplayName("추천 피드 컨트롤러 테스트")
class RecommendationControllerTest {

	private static final String USER_ID = "user-1";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewService reviewService;

	@MockitoBean
	private RecommendationService recommendationService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("로그인 사용자가 추천 피드를 조회한다")
	void getRecommendedReviews() throws Exception {
		when(recommendationService.getRecommendedReviews(USER_ID, 1, 10, null)).thenReturn(createPage());

		mockMvc.perform(get("/api/reviews/recommended?page=1&size=10").with(authentication(userAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value("review-1"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalElements").value(1));

		verify(recommendationService).getRecommendedReviews(USER_ID, 1, 10, null);
	}

	@Test
	@DisplayName("page, size가 범위를 벗어나면 400이다")
	void invalidPageParams() throws Exception {
		mockMvc.perform(get("/api/reviews/recommended?size=0").with(authentication(userAuthentication())))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reviews/recommended?size=101").with(authentication(userAuthentication())))
			.andExpect(status().isBadRequest());

		verify(recommendationService, never()).getRecommendedReviews(anyString(), anyInt(), anyInt(), any());
	}

	@Test
	@DisplayName("비로그인 요청은 401이고 인기 피드는 여전히 공개다")
	void unauthorizedButPopularStaysPublic() throws Exception {
		mockMvc.perform(get("/api/reviews/recommended"))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/reviews/popular"))
			.andExpect(status().isOk());

		verify(recommendationService, never()).getRecommendedReviews(anyString(), anyInt(), anyInt(), any());
	}

	private Authentication userAuthentication() {
		return new UsernamePasswordAuthenticationToken(
			USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	private RecommendationPageResponse createPage() {
		ReviewResponse review = new ReviewResponse();
		review.setId("review-1");
		RecommendationPageResponse page = new RecommendationPageResponse();
		page.setContent(List.of(review));
		page.setPage(1);
		page.setSize(10);
		page.setTotalElements(1);
		page.setTotalPages(1);
		page.setFirst(false);
		page.setLast(true);
		return page;
	}
}
