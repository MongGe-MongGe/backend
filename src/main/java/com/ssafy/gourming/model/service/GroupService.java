package com.ssafy.gourming.model.service;

import java.util.List;

import com.ssafy.gourming.model.dto.GroupDto.GroupCreateRequest;
import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupUpdateRequest;
import com.ssafy.gourming.model.dto.GroupDto.GroupWithUserResponse;

public interface GroupService {

	// 기본 그룹이 이미 있으면 기존 그룹을 반환
	GroupEntity createDefaultGroup(String userId);

	GroupResponse createGroup(String userId, GroupCreateRequest request);

	List<GroupResponse> getGroupsByUserId(String userId);

	/**
	 * 본인 및 팔로우하는 유저들의 그룹 목록을 조회합니다.
	 * (맛집이 없는 그룹은 결과에서 제외됩니다)
	 * 
	 * @param userId 로그인한 유저 ID
	 * @return 그룹 및 소유자 정보가 포함된 리스트
	 */
	List<GroupWithUserResponse> getFollowingGroups(String userId);

	GroupResponse updateGroup(String userId, String groupId, GroupUpdateRequest request);

	void deleteGroup(String userId, String groupId);
}
