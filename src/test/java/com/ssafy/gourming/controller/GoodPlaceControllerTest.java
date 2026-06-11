package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.PlaceSummary;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.service.GoodPlaceService;
import com.ssafy.gourming.security.LoginUser;

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

	@Test
	@DisplayName("인증 없이 다른 사용자의 그룹 맛집을 조회한다")
	void getGroupGoodPlacesWithoutAuthentication() throws Exception {
		when(goodPlaceService.getGroupGoodPlaces(OTHER_USER_ID, GROUP_ID))
			.thenReturn(List.of(createDetailResponse()));

		mockMvc.perform(get(
				"/api/users/{userId}/groups/{groupId}/good-places",
				OTHER_USER_ID,
				GROUP_ID
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value("good-place-1"))
			.andExpect(jsonPath("$[0].groupId").value(GROUP_ID))
			.andExpect(jsonPath("$[0].place.id").value(PLACE_ID))
			.andExpect(jsonPath("$[0].place.name").value("테스트 맛집"))
			.andExpect(jsonPath("$[0].place.categoryName").value("음식점"));

		verify(goodPlaceService).getGroupGoodPlaces(OTHER_USER_ID, GROUP_ID);
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
			.andExpect(status().isForbidden());

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
			.andExpect(status().isForbidden());

		verify(goodPlaceService, never()).deleteGoodPlaceFromGroup(any(), any(), any());
	}

	private Authentication loginAuthentication() {
		LoginUser loginUser = new LoginUser(USER_ID, "user@test.com");
		return new UsernamePasswordAuthenticationToken(loginUser, null, List.of());
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
		place.setCategoryName("음식점");
		place.setRoadAddressName("서울시 테스트로 1");
		place.setX("127.0");
		place.setY("37.0");

		GoodPlaceDetailResponse response = new GoodPlaceDetailResponse();
		response.setId("good-place-1");
		response.setGroupId(GROUP_ID);
		response.setPlace(place);
		return response;
	}
}
