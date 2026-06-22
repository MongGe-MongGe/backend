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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceCreateRequest;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceDetailResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlacePageResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.PlaceSummary;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.service.GoodPlaceService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(GoodPlaceController.class)
@Import(SecurityConfig.class)
@DisplayName("맛집 컨트롤러 테스트")
class GoodPlaceControllerTest {

	private static final String USER_ID = "user-1";
	private static final String OTHER_USER_ID = "user-2";
	private static final String GROUP_ID = "group-1";
	private static final String PLACE_ID = "place-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private GoodPlaceService goodPlaceService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("인증 없이 다른 사용자의 그룹 맛집을 조회한다")
	void getGroupGoodPlacesWithoutAuthentication() throws Exception {
		when(goodPlaceService.getGroupGoodPlaces(OTHER_USER_ID, GROUP_ID, 0, 20))
			.thenReturn(createPageResponse());

		mockMvc.perform(get(
				"/api/users/{userId}/groups/{groupId}/good-places",
				OTHER_USER_ID,
				GROUP_ID
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value("good-place-1"))
			.andExpect(jsonPath("$.content[0].groupId").value(GROUP_ID))
			.andExpect(jsonPath("$.content[0].place.id").value(PLACE_ID))
			.andExpect(jsonPath("$.content[0].place.categoryName")
				.value("음식점 > 한식"))
			.andExpect(jsonPath("$.content[0].place.categoryGroupCode").value("FD6"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(true));

		verify(goodPlaceService).getGroupGoodPlaces(OTHER_USER_ID, GROUP_ID, 0, 20);
	}

	@Test
	@DisplayName("잘못된 페이지 요청은 거부한다")
	void getGroupGoodPlacesWithInvalidPageFails() throws Exception {
		mockMvc.perform(get(
				"/api/users/{userId}/groups/{groupId}/good-places",
				OTHER_USER_ID,
				GROUP_ID
			)
				.param("page", "-1")
				.param("size", "101"))
			.andExpect(status().isBadRequest());

		verify(goodPlaceService, never())
			.getGroupGoodPlaces(any(), any(), anyInt(), anyInt());
	}

	@Test
	@DisplayName("인증된 사용자의 그룹에 맛집을 저장한다")
	void createGoodPlace() throws Exception {
		GoodPlaceCreateRequest request = createRequest();
		when(goodPlaceService.createGoodPlace(any(), any(), any()))
			.thenReturn(createResponse());

		mockMvc.perform(post("/api/users/me/groups/{groupId}/good-places", GROUP_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value("good-place-1"))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID))
			.andExpect(jsonPath("$.placeId").value(PLACE_ID));

		verify(goodPlaceService).createGoodPlace(
			eq(USER_ID),
			eq(GROUP_ID),
			org.mockito.ArgumentMatchers.argThat(goodPlaceRequest ->
				goodPlaceRequest.getPlace().getId().equals(PLACE_ID)
					&& goodPlaceRequest.getPlace().getName().equals("테스트 맛집")
			)
		);
	}

	@Test
	@DisplayName("인증 없이 맛집을 저장할 수 없다")
	void createGoodPlaceWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(post("/api/users/me/groups/{groupId}/good-places", GROUP_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest())))
			.andExpect(status().isUnauthorized());

		verify(goodPlaceService, never()).createGoodPlace(any(), any(), any());
	}

	@Test
	@DisplayName("장소 정보 없이 맛집을 저장할 수 없다")
	void createGoodPlaceWithoutPlaceFails() throws Exception {
		GoodPlaceCreateRequest request = new GoodPlaceCreateRequest();

		mockMvc.perform(post("/api/users/me/groups/{groupId}/good-places", GROUP_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(goodPlaceService, never()).createGoodPlace(any(), any(), any());
	}

	@Test
	@DisplayName("필수 장소 필드가 비어 있으면 맛집을 저장할 수 없다")
	void createGoodPlaceWithBlankPlaceFieldsFails() throws Exception {
		GoodPlaceCreateRequest request = new GoodPlaceCreateRequest();
		request.setPlace(new PlaceRequest());

		mockMvc.perform(post("/api/users/me/groups/{groupId}/good-places", GROUP_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(goodPlaceService, never()).createGoodPlace(any(), any(), any());
	}

	@Test
	@DisplayName("인증된 사용자의 그룹에서 맛집을 삭제한다")
	void deleteGoodPlaceFromGroup() throws Exception {
		mockMvc.perform(delete(
				"/api/users/me/groups/{groupId}/good-places/{placeId}",
				GROUP_ID,
				PLACE_ID
			)
				.with(authentication(loginAuthentication())))
			.andExpect(status().isNoContent());

		verify(goodPlaceService).deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID);
	}

	@Test
	@DisplayName("인증 없이 맛집을 삭제할 수 없다")
	void deleteGoodPlaceWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(delete(
				"/api/users/me/groups/{groupId}/good-places/{placeId}",
				GROUP_ID,
				PLACE_ID
			))
			.andExpect(status().isUnauthorized());

		verify(goodPlaceService, never()).deleteGoodPlaceFromGroup(any(), any(), any());
	}

	private Authentication loginAuthentication() {
		return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
	}

	private GoodPlaceCreateRequest createRequest() {
		PlaceRequest place = new PlaceRequest();
		place.setId(PLACE_ID);
		place.setName("테스트 맛집");
		place.setX("127.0");
		place.setY("37.0");

		GoodPlaceCreateRequest request = new GoodPlaceCreateRequest();
		request.setPlace(place);
		return request;
	}

	private GoodPlaceResponse createResponse() {
		GoodPlaceResponse response = new GoodPlaceResponse();
		response.setId("good-place-1");
		response.setGroupId(GROUP_ID);
		response.setPlaceId(PLACE_ID);
		return response;
	}

	private GoodPlaceDetailResponse createDetailResponse() {
		PlaceSummary place = new PlaceSummary();
		place.setId(PLACE_ID);
		place.setName("테스트 맛집");
		place.setCategoryName("음식점 > 한식");
		place.setCategoryGroupCode("FD6");
		place.setRoadAddressName("서울시 테스트로 1");
		place.setX("127.0");
		place.setY("37.0");

		GoodPlaceDetailResponse response = new GoodPlaceDetailResponse();
		response.setId("good-place-1");
		response.setGroupId(GROUP_ID);
		response.setPlace(place);
		return response;
	}

	private GoodPlacePageResponse createPageResponse() {
		GoodPlacePageResponse response = new GoodPlacePageResponse();
		response.setContent(List.of(createDetailResponse()));
		response.setPage(0);
		response.setSize(20);
		response.setTotalElements(1);
		response.setTotalPages(1);
		response.setFirst(true);
		response.setLast(true);
		return response;
	}
}
