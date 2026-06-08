package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupCreateRequest;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupUpdateRequest;
import com.ssafy.gourming.model.mapper.GroupMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("그룹 서비스 Mock 단위 테스트")
class GroupServiceMockTest {

	private static final String USER_ID = "user-1";
	private static final String OTHER_USER_ID = "user-2";
	private static final String GROUP_ID = "group-1";

	@Mock
	private GroupMapper groupMapper;

	@InjectMocks
	private GroupServiceImpl groupService;

	@Test
	@DisplayName("기본 그룹이 이미 있으면 기존 그룹을 반환한다")
	void createDefaultGroupReturnsExistingGroup() {
		GroupEntity existingGroup = createGroup(USER_ID, true);
		when(groupMapper.selectDefaultGroupByUserId(USER_ID)).thenReturn(existingGroup);

		GroupEntity result = groupService.createDefaultGroup(USER_ID);

		assertThat(result).isSameAs(existingGroup);
		verify(groupMapper, never()).insertGroup(any());
	}

	@Test
	@DisplayName("기본 그룹이 없으면 생성하고 반환한다")
	void createDefaultGroup() {
		GroupEntity createdGroup = createGroup(USER_ID, true);
		when(groupMapper.selectDefaultGroupByUserId(USER_ID)).thenReturn(null, createdGroup);
		when(groupMapper.insertGroup(any())).thenReturn(1);

		GroupEntity result = groupService.createDefaultGroup(USER_ID);

		assertThat(result).isSameAs(createdGroup);
		verify(groupMapper).insertGroup(argThat(group ->
			group.getUserId().equals(USER_ID)
				&& group.getName().equals("내 장소")
				&& group.isDefaultGroup()
		));
	}

	@Test
	@DisplayName("앞뒤 공백을 제거한 이름으로 일반 그룹을 생성한다")
	void createGroup() {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("  친구 추천  ");
		GroupEntity createdGroup = createGroup(USER_ID, false);
		createdGroup.setName("친구 추천");

		when(groupMapper.selectGroupByUserIdAndName(USER_ID, "친구 추천"))
			.thenReturn(null, createdGroup);
		when(groupMapper.insertGroup(any())).thenReturn(1);

		GroupResponse result = groupService.createGroup(USER_ID, request);

		assertThat(result.getName()).isEqualTo("친구 추천");
		verify(groupMapper).insertGroup(argThat(group ->
			group.getUserId().equals(USER_ID)
				&& group.getName().equals("친구 추천")
				&& !group.isDefaultGroup()
		));
	}

	@Test
	@DisplayName("공백으로만 이루어진 그룹 이름은 생성할 수 없다")
	void createBlankGroupFails() {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("   ");

		assertThatThrownBy(() -> groupService.createGroup(USER_ID, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Group name cannot be blank");
		verify(groupMapper, never()).insertGroup(any());
	}

	@Test
	@DisplayName("같은 이름의 일반 그룹이 있으면 생성할 수 없다")
	void createDuplicateGroupFails() {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("친구 추천");
		when(groupMapper.selectGroupByUserIdAndName(USER_ID, "친구 추천"))
			.thenReturn(createGroup(USER_ID, false));

		assertThatThrownBy(() -> groupService.createGroup(USER_ID, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Already Exists Group");
		verify(groupMapper, never()).insertGroup(any());
	}

	@Test
	@DisplayName("일반 그룹 생성이 DB에 반영되지 않으면 실패한다")
	void createGroupInsertFails() {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName("친구 추천");
		when(groupMapper.selectGroupByUserIdAndName(USER_ID, "친구 추천")).thenReturn(null);
		when(groupMapper.insertGroup(any())).thenReturn(0);

		assertThatThrownBy(() -> groupService.createGroup(USER_ID, request))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to create group");
	}

	@Test
	@DisplayName("기본 그룹 생성이 DB에 반영되지 않으면 실패한다")
	void createDefaultGroupInsertFails() {
		when(groupMapper.selectDefaultGroupByUserId(USER_ID)).thenReturn(null);
		when(groupMapper.insertGroup(any())).thenReturn(0);

		assertThatThrownBy(() -> groupService.createDefaultGroup(USER_ID))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to create default group");
	}

	@Test
	@DisplayName("그룹 이름이 null이면 생성할 수 없다")
	void createNullNameGroupFails() {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(null);

		assertThatThrownBy(() -> groupService.createGroup(USER_ID, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Group name cannot be blank");
		verify(groupMapper, never()).insertGroup(any());
	}

	@Test
	@DisplayName("소유한 일반 그룹의 이름을 수정한다")
	void updateGroup() {
		GroupEntity group = createGroup(USER_ID, false);
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("  수정한 그룹  ");
		GroupResponse response = createResponse("수정한 그룹", 3);

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);
		when(groupMapper.selectGroupByUserIdAndName(USER_ID, "수정한 그룹")).thenReturn(null);
		when(groupMapper.updateGroupName(GROUP_ID, USER_ID, "수정한 그룹")).thenReturn(1);
		when(groupMapper.selectGroupsByUserId(USER_ID)).thenReturn(List.of(response));

		GroupResponse result = groupService.updateGroup(USER_ID, GROUP_ID, request);

		assertThat(result).isSameAs(response);
		assertThat(result.getGoodPlaceCount()).isEqualTo(3);
		verify(groupMapper).updateGroupName(GROUP_ID, USER_ID, "수정한 그룹");
	}

	@Test
	@DisplayName("다른 그룹과 이름이 중복되면 수정할 수 없다")
	void updateGroupToDuplicateNameFails() {
		GroupEntity group = createGroup(USER_ID, false);
		GroupEntity groupWithSameName = createGroup(USER_ID, false);
		groupWithSameName.setId("group-2");
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("중복 그룹");

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);
		when(groupMapper.selectGroupByUserIdAndName(USER_ID, "중복 그룹"))
			.thenReturn(groupWithSameName);

		assertThatThrownBy(() -> groupService.updateGroup(USER_ID, GROUP_ID, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Already Exists Group");
		verify(groupMapper, never()).updateGroupName(GROUP_ID, USER_ID, "중복 그룹");
	}

	@Test
	@DisplayName("현재 그룹과 같은 이름으로 수정할 수 있다")
	void updateGroupWithCurrentName() {
		GroupEntity group = createGroup(USER_ID, false);
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("그룹");
		GroupResponse response = createResponse("그룹", 2);

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);
		when(groupMapper.selectGroupByUserIdAndName(USER_ID, "그룹")).thenReturn(group);
		when(groupMapper.updateGroupName(GROUP_ID, USER_ID, "그룹")).thenReturn(1);
		when(groupMapper.selectGroupsByUserId(USER_ID)).thenReturn(List.of(response));

		GroupResponse result = groupService.updateGroup(USER_ID, GROUP_ID, request);

		assertThat(result).isSameAs(response);
		verify(groupMapper).updateGroupName(GROUP_ID, USER_ID, "그룹");
	}

	@Test
	@DisplayName("그룹 이름 수정이 DB에 반영되지 않으면 실패한다")
	void updateGroupDatabaseFails() {
		GroupEntity group = createGroup(USER_ID, false);
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("수정한 그룹");

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);
		when(groupMapper.selectGroupByUserIdAndName(USER_ID, "수정한 그룹")).thenReturn(null);
		when(groupMapper.updateGroupName(GROUP_ID, USER_ID, "수정한 그룹")).thenReturn(0);

		assertThatThrownBy(() -> groupService.updateGroup(USER_ID, GROUP_ID, request))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to update group: " + GROUP_ID);
	}

	@Test
	@DisplayName("기본 그룹은 수정할 수 없다")
	void updateDefaultGroupFails() {
		GroupEntity group = createGroup(USER_ID, true);
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("수정한 그룹");

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);

		assertThatThrownBy(() -> groupService.updateGroup(USER_ID, GROUP_ID, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Default group cannot be updated");
		verify(groupMapper, never()).updateGroupName(GROUP_ID, USER_ID, "수정한 그룹");
	}

	@Test
	@DisplayName("다른 사용자의 그룹은 수정할 수 없다")
	void updateOtherUsersGroupFails() {
		GroupEntity group = createGroup(OTHER_USER_ID, false);
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("수정한 그룹");

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);

		assertThatThrownBy(() -> groupService.updateGroup(USER_ID, GROUP_ID, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Group does not belong to user");
		verify(groupMapper, never()).updateGroupName(GROUP_ID, USER_ID, "수정한 그룹");
	}

	@Test
	@DisplayName("존재하지 않는 그룹은 수정할 수 없다")
	void updateMissingGroupFails() {
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName("수정한 그룹");
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(null);

		assertThatThrownBy(() -> groupService.updateGroup(USER_ID, GROUP_ID, request))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Group not found: " + GROUP_ID);
	}

	@Test
	@DisplayName("소유한 일반 그룹을 삭제한다")
	void deleteGroup() {
		GroupEntity group = createGroup(USER_ID, false);
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);
		when(groupMapper.deleteGroup(GROUP_ID, USER_ID)).thenReturn(1);

		groupService.deleteGroup(USER_ID, GROUP_ID);

		verify(groupMapper).deleteGroup(GROUP_ID, USER_ID);
	}

	@Test
	@DisplayName("기본 그룹은 삭제할 수 없다")
	void deleteDefaultGroupFails() {
		GroupEntity group = createGroup(USER_ID, true);
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);

		assertThatThrownBy(() -> groupService.deleteGroup(USER_ID, GROUP_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Default group cannot be deleted");
		verify(groupMapper, never()).deleteGroup(GROUP_ID, USER_ID);
	}

	@Test
	@DisplayName("다른 사용자의 그룹은 삭제할 수 없다")
	void deleteOtherUsersGroupFails() {
		GroupEntity group = createGroup(OTHER_USER_ID, false);
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);

		assertThatThrownBy(() -> groupService.deleteGroup(USER_ID, GROUP_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Group does not belong to user");
		verify(groupMapper, never()).deleteGroup(GROUP_ID, USER_ID);
	}

	@Test
	@DisplayName("존재하지 않는 그룹은 삭제할 수 없다")
	void deleteMissingGroupFails() {
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(null);

		assertThatThrownBy(() -> groupService.deleteGroup(USER_ID, GROUP_ID))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Group not found: " + GROUP_ID);
		verify(groupMapper, never()).deleteGroup(GROUP_ID, USER_ID);
	}

	@Test
	@DisplayName("그룹 삭제가 DB에 반영되지 않으면 실패한다")
	void deleteGroupDatabaseFails() {
		GroupEntity group = createGroup(USER_ID, false);
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(group);
		when(groupMapper.deleteGroup(GROUP_ID, USER_ID)).thenReturn(0);

		assertThatThrownBy(() -> groupService.deleteGroup(USER_ID, GROUP_ID))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to delete group: " + GROUP_ID);
	}

	private GroupEntity createGroup(String userId, boolean defaultGroup) {
		GroupEntity group = new GroupEntity();
		group.setId(GROUP_ID);
		group.setUserId(userId);
		group.setName("그룹");
		group.setDefaultGroup(defaultGroup);
		return group;
	}

	private GroupResponse createResponse(String name, int goodPlaceCount) {
		GroupResponse response = new GroupResponse();
		response.setId(GROUP_ID);
		response.setName(name);
		response.setGoodPlaceCount(goodPlaceCount);
		return response;
	}
}
