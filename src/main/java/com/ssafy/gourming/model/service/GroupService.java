package com.ssafy.gourming.model.service;

import java.util.List;

import com.ssafy.gourming.model.dto.GroupDto.GroupCreateRequest;
import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupUpdateRequest;

public interface GroupService {

	// 기본 그룹이 이미 있으면 기존 그룹을 반환
	GroupEntity createDefaultGroup(String userId);

	GroupResponse createGroup(String userId, GroupCreateRequest request);

	List<GroupResponse> getGroupsByUserId(String userId);

	GroupResponse updateGroup(String userId, String groupId, GroupUpdateRequest request);

	void deleteGroup(String userId, String groupId);
}
