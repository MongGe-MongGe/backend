package com.ssafy.gourming.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingRefreshResponse;
import com.ssafy.gourming.model.service.EmbeddingService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(EmbeddingController.class)
@Import(SecurityConfig.class)
@DisplayName("임베딩 컨트롤러 테스트")
class EmbeddingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmbeddingService embeddingService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("관리자가 전체 벡터를 재계산한다")
	void refreshAsAdmin() throws Exception {
		when(embeddingService.isRunning()).thenReturn(false);
		when(embeddingService.refreshAll()).thenReturn(createResponse());

		mockMvc.perform(put("/api/embeddings/refresh").with(authentication(auth("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reviewRequestedCount").value(3))
			.andExpect(jsonPath("$.reviewSuccessCount").value(2))
			.andExpect(jsonPath("$.embedderVersion").value("test-v1"));

		verify(embeddingService).refreshAll();
	}

	@Test
	@DisplayName("실행 중이면 409를 반환한다")
	void refreshConflictWhenRunning() throws Exception {
		when(embeddingService.isRunning()).thenReturn(true);

		mockMvc.perform(put("/api/embeddings/refresh").with(authentication(auth("ROLE_ADMIN"))))
			.andExpect(status().isConflict());

		verify(embeddingService, never()).refreshAll();
	}

	@Test
	@DisplayName("일반 사용자는 403, 비로그인은 401이다")
	void refreshForbiddenForUser() throws Exception {
		mockMvc.perform(put("/api/embeddings/refresh").with(authentication(auth("ROLE_USER"))))
			.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/embeddings/refresh"))
			.andExpect(status().isUnauthorized());

		verify(embeddingService, never()).refreshAll();
	}

	private Authentication auth(String role) {
		return new UsernamePasswordAuthenticationToken(
			"user-1", null, List.of(new SimpleGrantedAuthority(role)));
	}

	private EmbeddingRefreshResponse createResponse() {
		EmbeddingRefreshResponse response = new EmbeddingRefreshResponse();
		response.setReviewRequestedCount(3);
		response.setReviewSuccessCount(2);
		response.setReviewFailedCount(1);
		response.setEmbedderVersion("test-v1");
		response.setStartedAt(LocalDateTime.of(2026, 9, 30, 0, 5));
		response.setFinishedAt(LocalDateTime.of(2026, 9, 30, 0, 6));
		return response;
	}
}
