package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryBulkRefreshResponse;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryResponse;
import com.ssafy.gourming.model.service.PlaceReviewSummaryService;
import com.ssafy.gourming.model.service.PlaceService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(PlaceController.class)
@Import(SecurityConfig.class)
@DisplayName("장소 컨트롤러 테스트")
class PlaceControllerTest {

	private static final String USER_ID = "user-1";
	private static final String PLACE_ID = "place-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PlaceService placeService;

	@MockitoBean
	private com.ssafy.gourming.model.service.GoodPlaceService goodPlaceService;

	@MockitoBean
	private PlaceReviewSummaryService placeReviewSummaryService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("인증 없이 카카오 장소를 검증/저장하고 저장된 리뷰 요약을 함께 반환한다")
	void checkOrCreatePlaceWithSummary() throws Exception {
		when(placeService.findOrCreatePlace(any())).thenReturn(createPlace());
		when(placeReviewSummaryService.getSummary(PLACE_ID))
			.thenReturn(createSummary());

		mockMvc.perform(post("/api/places")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createPlaceRequest())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(PLACE_ID))
			.andExpect(jsonPath("$.name").value("테스트 장소"))
			.andExpect(jsonPath("$.reviewSummary.summary").value("리뷰 요약입니다."));

		verify(placeService).findOrCreatePlace(org.mockito.ArgumentMatchers.argThat(request ->
			request.getId().equals(PLACE_ID)
				&& request.getName().equals("테스트 장소")
		));
		verify(placeReviewSummaryService).getSummary(PLACE_ID);
		verify(placeReviewSummaryService, never()).refreshSummary(any());
	}

	@Test
	@DisplayName("저장된 요약이 없으면 장소 검증/저장 응답의 reviewSummary는 null이다")
	void checkOrCreatePlaceWithoutSummary() throws Exception {
		when(placeService.findOrCreatePlace(any())).thenReturn(createPlace());
		when(placeReviewSummaryService.getSummary(PLACE_ID)).thenReturn(null);

		mockMvc.perform(post("/api/places")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createPlaceRequest())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(PLACE_ID))
			.andExpect(jsonPath("$.reviewSummary").doesNotExist());

		verify(placeReviewSummaryService).getSummary(PLACE_ID);
		verify(placeReviewSummaryService, never()).refreshSummary(any());
	}

	@Test
	@DisplayName("장소 검증/저장 결과가 없으면 404를 반환한다")
	void checkOrCreateMissingPlaceFails() throws Exception {
		when(placeService.findOrCreatePlace(any())).thenReturn(null);

		mockMvc.perform(post("/api/places")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createPlaceRequest())))
			.andExpect(status().isNotFound());

		verify(placeReviewSummaryService, never()).getSummary(any());
		verify(placeReviewSummaryService, never()).refreshSummary(any());
	}

	@Test
	@DisplayName("관리자가 단일 장소 리뷰 요약을 강제 갱신한다")
	void refreshSummary() throws Exception {
		when(placeReviewSummaryService.refreshSummary(PLACE_ID))
			.thenReturn(createSummary());

		mockMvc.perform(put("/api/places/summary/{placeId}", PLACE_ID)
				.with(authentication(adminAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.placeId").value(PLACE_ID))
			.andExpect(jsonPath("$.summary").value("리뷰 요약입니다."))
			.andExpect(jsonPath("$.status").value("COMPLETED"));

		verify(placeReviewSummaryService).refreshSummary(PLACE_ID);
	}

	@Test
	@DisplayName("관리자가 전체 장소 리뷰 요약을 강제 갱신한다")
	void refreshAllSummaries() throws Exception {
		when(placeReviewSummaryService.refreshAllSummaries())
			.thenReturn(createBulkRefreshResponse());

		mockMvc.perform(put("/api/places/summary")
				.with(authentication(adminAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requestedCount").value(2))
			.andExpect(jsonPath("$.successCount").value(1))
			.andExpect(jsonPath("$.failedCount").value(1))
			.andExpect(jsonPath("$.failedPlaceIds[0]").value("failed-place"));

		verify(placeReviewSummaryService).refreshAllSummaries();
	}

	@Test
	@DisplayName("인증 없이 장소 리뷰 요약을 갱신할 수 없다")
	void refreshSummaryWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(put("/api/places/summary/{placeId}", PLACE_ID))
			.andExpect(status().isUnauthorized());

		verify(placeReviewSummaryService, never()).refreshSummary(any());
	}

	@Test
	@DisplayName("일반 사용자는 장소 리뷰 요약을 갱신할 수 없다")
	void refreshSummaryWithUserRoleFails() throws Exception {
		mockMvc.perform(put("/api/places/summary/{placeId}", PLACE_ID)
				.with(authentication(userAuthentication())))
			.andExpect(status().isForbidden());

		verify(placeReviewSummaryService, never()).refreshSummary(any());
	}

	private Authentication userAuthentication() {
		return new UsernamePasswordAuthenticationToken(
			USER_ID,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}

	private Authentication adminAuthentication() {
		return new UsernamePasswordAuthenticationToken(
			USER_ID,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
		);
	}

	private PlaceEntity createPlace() {
		PlaceEntity place = new PlaceEntity();
		place.setId(PLACE_ID);
		place.setName("테스트 장소");
		place.setCategoryName("음식점 > 양식");
		place.setCategoryGroupCode("FD6");
		place.setRoadAddressName("서울시 테스트로 1");
		place.setX("127.000000");
		place.setY("37.000000");
		return place;
	}

	private PlaceRequest createPlaceRequest() {
		PlaceRequest request = new PlaceRequest();
		request.setId(PLACE_ID);
		request.setName("테스트 장소");
		request.setX("127.000000");
		request.setY("37.000000");
		request.setRoadAddressName("서울시 테스트로 1");
		request.setCategoryName("음식점 > 양식");
		return request;
	}

	private PlaceReviewSummaryResponse createSummary() {
		PlaceReviewSummaryResponse response = new PlaceReviewSummaryResponse();
		response.setPlaceId(PLACE_ID);
		response.setSummary("리뷰 요약입니다.");
		response.setPositivePoints(List.of("맛이 좋아요"));
		response.setNegativePoints(List.of("대기가 길 수 있어요"));
		response.setRecommendedFor(List.of("데이트"));
		response.setKeywords(List.of("파스타", "분위기"));
		response.setReviewCount(3);
		response.setStatus("COMPLETED");
		response.setLastReviewUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 0));
		response.setUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 10));
		return response;
	}

	private PlaceReviewSummaryBulkRefreshResponse createBulkRefreshResponse() {
		PlaceReviewSummaryBulkRefreshResponse response =
			new PlaceReviewSummaryBulkRefreshResponse();
		response.setRequestedCount(2);
		response.setSuccessCount(1);
		response.setFailedCount(1);
		response.setFailedPlaceIds(List.of("failed-place"));
		response.setStartedAt(LocalDateTime.of(2026, 6, 24, 12, 0));
		response.setFinishedAt(LocalDateTime.of(2026, 6, 24, 12, 10));
		return response;
	}
}
