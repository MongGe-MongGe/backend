package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.ssafy.gourming.model.dto.GroupDto.GroupCreateRequest;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupUpdateRequest;
import com.ssafy.gourming.model.service.GroupService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(GroupController.class)
@Import(SecurityConfig.class)
@DisplayName("그룹 컨트롤러 테스트")
class GroupControllerTest {

	private static final String USER_ID = "user-1";
	private static final String OTHER_USER_ID = "user-2";
	private static final String GROUP_ID = "group-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private GroupService groupService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("인증 없이 내 그룹 목록을 조회할 수 없다")
	void getMyGroupsWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(get("/api/users/me/groups"))
			.andExpect(status().isUnauthorized());

		verify(groupService, never()).getGroupsByUserId(any());
	}

	@Test
	@DisplayName("내 그룹 목록을 조회한다")
	void getMyGroups() throws Exception {
		when(groupService.getGroupsByUserId(USER_ID)).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/users/me/groups").with(authentication(loginAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(GROUP_ID))
			.andExpect(jsonPath("$[0].name").value("친구 추천"))
			.andExpect(jsonPath("$[0].goodPlaceCount").value(2));

		verify(groupService).getGroupsByUserId(USER_ID);
	}

	@Test
	@DisplayName("다른 사용자의 그룹 목록을 조회한다")
	void getUserGroups() throws Exception {
		when(groupService.getGroupsByUserId(OTHER_USER_ID))
			.thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/users/{userId}/groups", OTHER_USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(GROUP_ID));

		verify(groupService).getGroupsByUserId(OTHER_USER_ID);
	}

	@Test
	@DisplayName("그룹을 생성한다")
	void createGroup() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("친구 추천");
		when(groupService.createGroup(any(), any())).thenReturn(createResponse());

		mockMvc.perform(post("/api/users/me/groups")
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(GROUP_ID))
			.andExpect(jsonPath("$.name").value("친구 추천"));

		verify(groupService).createGroup(
			org.mockito.ArgumentMatchers.eq(USER_ID),
			org.mockito.ArgumentMatchers.argThat(groupRequest ->
				groupRequest.getName().equals("친구 추천")
			)
		);
	}

	@Test
	@DisplayName("인증 없이 그룹을 생성할 수 없다")
	void createGroupWithoutAuthenticationFails() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("친구 추천");

		mockMvc.perform(post("/api/users/me/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(groupService, never()).createGroup(any(), any());
	}

	@Test
	@DisplayName("빈 이름으로 그룹을 생성할 수 없다")
	void createGroupWithBlankNameFails() throws Exception {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(" ");

		mockMvc.perform(post("/api/users/me/groups")
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(groupService, never()).createGroup(any(), any());
	}

	@Test
	@DisplayName("그룹 이름을 수정한다")
	void updateGroup() throws Exception {
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("데이트");
		GroupResponse response = createResponse();
		response.setName("데이트");
		when(groupService.updateGroup(any(), any(), any())).thenReturn(response);

		mockMvc.perform(put("/api/users/me/groups/{groupId}", GROUP_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(GROUP_ID))
			.andExpect(jsonPath("$.name").value("데이트"));

		verify(groupService).updateGroup(
			org.mockito.ArgumentMatchers.eq(USER_ID),
			org.mockito.ArgumentMatchers.eq(GROUP_ID),
			org.mockito.ArgumentMatchers.argThat(groupRequest ->
				groupRequest.getName().equals("데이트")
			)
		);
	}

	@Test
	@DisplayName("인증 없이 그룹을 수정할 수 없다")
	void updateGroupWithoutAuthenticationFails() throws Exception {
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("데이트");

		mockMvc.perform(put("/api/users/me/groups/{groupId}", GROUP_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(groupService, never()).updateGroup(any(), any(), any());
	}

	@Test
	@DisplayName("빈 이름으로 그룹을 수정할 수 없다")
	void updateGroupWithBlankNameFails() throws Exception {
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(" ");

		mockMvc.perform(put("/api/users/me/groups/{groupId}", GROUP_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(groupService, never()).updateGroup(any(), any(), any());
	}

	@Test
	@DisplayName("그룹을 삭제한다")
	void deleteGroup() throws Exception {
		mockMvc.perform(delete("/api/users/me/groups/{groupId}", GROUP_ID)
				.with(authentication(loginAuthentication())))
			.andExpect(status().isNoContent());

		verify(groupService).deleteGroup(USER_ID, GROUP_ID);
	}

	@Test
	@DisplayName("인증 없이 그룹을 삭제할 수 없다")
	void deleteGroupWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(delete("/api/users/me/groups/{groupId}", GROUP_ID))
			.andExpect(status().isUnauthorized());

		verify(groupService, never()).deleteGroup(any(), any());
	}

	private Authentication loginAuthentication() {
		return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
	}

	private GroupResponse createResponse() {
		GroupResponse response = new GroupResponse();
		response.setId(GROUP_ID);
		response.setName("친구 추천");
		response.setDefaultGroup(false);
		response.setGoodPlaceCount(2);
		return response;
	}
}
