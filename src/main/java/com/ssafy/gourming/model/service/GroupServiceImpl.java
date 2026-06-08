package com.ssafy.gourming.model.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.GroupDto.GroupCreateRequest;
import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupUpdateRequest;
import com.ssafy.gourming.model.mapper.GroupMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService{

	private final GroupMapper groupMapper;
	private static final String DEFAULT_GROUP_NAME = "내 장소";
	
	@Override
	@Transactional
	public GroupEntity createDefaultGroup(String userId) {
		GroupEntity existingGroup = groupMapper.selectDefaultGroupByUserId(userId);
		if(existingGroup != null) {
			return existingGroup;
		}
		
		GroupEntity newDefaultGroup = new GroupEntity();
		newDefaultGroup.setUserId(userId);
		newDefaultGroup.setName(DEFAULT_GROUP_NAME);
		newDefaultGroup.setDefaultGroup(true);
		
		int insertedCount = groupMapper.insertGroup(newDefaultGroup);
		if (insertedCount == 0) {
			throw new IllegalStateException("Failed to create default group");
		}

		GroupEntity createdGroup = groupMapper.selectDefaultGroupByUserId(userId);
		if (createdGroup == null) {
			throw new IllegalStateException("Failed to find created default group");
		}
		return createdGroup;
	}
	

	private GroupResponse convertToGroupResponse(GroupEntity group) {
	    GroupResponse response = new GroupResponse();
	    response.setId(group.getId());
	    response.setName(group.getName());
	    response.setDefaultGroup(group.isDefaultGroup());
	    response.setGoodPlaceCount(0); //그룹 생성 직후이므로 0
	    response.setCreatedAt(group.getCreatedAt());
	    return response;
	}

	@Override
	@Transactional
	public GroupResponse createGroup(String userId, GroupCreateRequest request) {
		String name = normalizeGroupName(request.getName());
		GroupEntity existingGroup = groupMapper.selectGroupByUserIdAndName(userId, name);
		if(existingGroup != null) {
			throw new IllegalArgumentException("Already Exists Group");
		}
		
		GroupEntity group = new GroupEntity();
		group.setUserId(userId);
		group.setName(name);
		
		int insertedCount = groupMapper.insertGroup(group);
		if (insertedCount == 0) {
			throw new IllegalStateException("Failed to create group");
		}

		GroupEntity createdGroup = groupMapper.selectGroupByUserIdAndName(userId, name);
		if (createdGroup == null) {
			throw new IllegalStateException("Failed to find created group");
		}
		return convertToGroupResponse(createdGroup);
	}

	@Override
	public List<GroupResponse> getMyGroups(String userId) {
		return groupMapper.selectGroupsByUserId(userId);
	}

	@Override
	public List<GroupResponse> getUserGroups(String userId) {
		return groupMapper.selectGroupsByUserId(userId);
	}

	@Override
	@Transactional
	public GroupResponse updateGroup(String userId, String groupId, GroupUpdateRequest request) {
		GroupEntity group = getGroup(groupId);
		validateOwner(group, userId);

		if (group.isDefaultGroup()) {
			throw new IllegalArgumentException("Default group cannot be updated");
		}

		String name = normalizeGroupName(request.getName());
		GroupEntity groupWithSameName = groupMapper.selectGroupByUserIdAndName(userId, name);
		if (groupWithSameName != null && !groupWithSameName.getId().equals(groupId)) {
			throw new IllegalArgumentException("Already Exists Group");
		}

		int updatedCount = groupMapper.updateGroupName(groupId, userId, name);
		if (updatedCount == 0) {
			throw new IllegalStateException("Failed to update group: " + groupId);
		}

		return findGroupResponseById(userId, groupId);
	}

	@Override
	@Transactional
	public void deleteGroup(String userId, String groupId) {
		GroupEntity group = getGroup(groupId);
		validateOwner(group, userId);

		if (group.isDefaultGroup()) {
			throw new IllegalArgumentException("Default group cannot be deleted");
		}

		int deletedCount = groupMapper.deleteGroup(groupId, userId);
		if (deletedCount == 0) {
			throw new IllegalStateException("Failed to delete group: " + groupId);
		}
	}

	private GroupEntity getGroup(String groupId) {
		GroupEntity group = groupMapper.selectGroupById(groupId);
		if (group == null) {
			throw new NoSuchElementException("Group not found: " + groupId);
		}
		return group;
	}

	private void validateOwner(GroupEntity group, String userId) {
		if (!group.getUserId().equals(userId)) {
			throw new IllegalArgumentException("Group does not belong to user");
		}
	}

	private GroupResponse findGroupResponseById(String userId, String groupId) {
		List<GroupResponse> groups = groupMapper.selectGroupsByUserId(userId);
		for (GroupResponse group : groups) {
			if (group.getId().equals(groupId)) {
				return group;
			}
		}
		throw new NoSuchElementException("Group not found: " + groupId);
	}

	private String normalizeGroupName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Group name cannot be blank");
		}
		String normalizedName = name.strip();
		if (normalizedName.isEmpty()) {
			throw new IllegalArgumentException("Group name cannot be blank");
		}
		return normalizedName;
	}

}
