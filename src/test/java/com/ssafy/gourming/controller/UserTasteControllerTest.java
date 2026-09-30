package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteProfileResponse;
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteTagScore;
import com.ssafy.gourming.model.service.FollowService;
import com.ssafy.gourming.model.service.UserService;
import com.ssafy.gourming.model.service.UserTasteService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("내 미식 성향 컨트롤러 테스트")
class UserTasteControllerTest {

	private static final String USER_ID = "user-1";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private FollowService followService;

	@MockitoBean
	private UserTasteService userTasteService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("로그인 사용자가 자신의 성향을 조회한다")
	void getTasteProfile() throws Exception {
		when(userTasteService.getTasteProfile(USER_ID)).thenReturn(createResponse());

		mockMvc.perform(get("/api/users/me/taste-profile").with(authentication(userAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.evidenceCount").value(12))
			.andExpect(jsonPath("$.confidence").value(0.71))
			.andExpect(jsonPath("$.topTags[0].code").value("dessert"))
			.andExpect(jsonPath("$.topTags[0].label").value("디저트"))
			.andExpect(jsonPath("$.topTags[0].category").value("CATEGORY"))
			.andExpect(jsonPath("$.topTags[0].score").value(0.74));

		verify(userTasteService).getTasteProfile(USER_ID);
	}

	@Test
	@DisplayName("비로그인 요청은 401이다")
	void getTasteProfileUnauthorized() throws Exception {
		mockMvc.perform(get("/api/users/me/taste-profile"))
			.andExpect(status().isUnauthorized());

		verify(userTasteService, never()).getTasteProfile(any());
	}

	private Authentication userAuthentication() {
		return new UsernamePasswordAuthenticationToken(
			USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	private UserTasteProfileResponse createResponse() {
		UserTasteTagScore tag = new UserTasteTagScore();
		tag.setCode("dessert");
		tag.setLabel("디저트");
		tag.setCategory("CATEGORY");
		tag.setScore(0.74);
		UserTasteProfileResponse response = new UserTasteProfileResponse();
		response.setEvidenceCount(12);
		response.setConfidence(0.71);
		response.setTopTags(List.of(tag));
		return response;
	}
}
