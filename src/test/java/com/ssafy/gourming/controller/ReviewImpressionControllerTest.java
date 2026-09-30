package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.service.ReviewImpressionService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(ReviewImpressionController.class)
@Import(SecurityConfig.class)
@DisplayName("리뷰 노출 기록 컨트롤러 테스트")
class ReviewImpressionControllerTest {

	private static final String USER_ID = "user-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ReviewImpressionService reviewImpressionService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("로그인 사용자가 노출을 기록하면 204를 반환한다")
	void recordImpressions() throws Exception {
		mockMvc.perform(post("/api/reviews/impressions")
				.with(authentication(userAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(List.of("r1", "r2"))))
			.andExpect(status().isNoContent());

		verify(reviewImpressionService).record(USER_ID, List.of("r1", "r2"));
	}

	@Test
	@DisplayName("빈 목록이나 100개 초과는 400이다")
	void rejectsInvalidSize() throws Exception {
		mockMvc.perform(post("/api/reviews/impressions")
				.with(authentication(userAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(List.of())))
			.andExpect(status().isBadRequest());

		List<String> tooMany = IntStream.range(0, 101).mapToObj(i -> "r" + i).toList();
		mockMvc.perform(post("/api/reviews/impressions")
				.with(authentication(userAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(tooMany)))
			.andExpect(status().isBadRequest());

		verify(reviewImpressionService, never()).record(any(), anyList());
	}

	@Test
	@DisplayName("비로그인 요청은 401이다")
	void unauthorized() throws Exception {
		mockMvc.perform(post("/api/reviews/impressions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(List.of("r1"))))
			.andExpect(status().isUnauthorized());

		verify(reviewImpressionService, never()).record(any(), anyList());
	}

	private String body(List<String> reviewIds) throws Exception {
		return objectMapper.writeValueAsString(Map.of("reviewIds", reviewIds));
	}

	private Authentication userAuthentication() {
		return new UsernamePasswordAuthenticationToken(
			USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}
}
