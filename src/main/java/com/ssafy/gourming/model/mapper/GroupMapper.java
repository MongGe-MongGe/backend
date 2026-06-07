package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;

@Mapper
public interface GroupMapper {

	int insertGroup(GroupEntity group);

	GroupEntity selectGroupById(String groupId);

	GroupEntity selectDefaultGroupByUserId(String userId);

	List<GroupResponse> selectGroupsByUserId(String userId);

	GroupEntity selectGroupByUserIdAndName(
		@Param("userId") String userId,
		@Param("name") String name
	);

	int updateGroupName(
		@Param("groupId") String groupId,
		@Param("userId") String userId,
		@Param("name") String name
	);

	int deleteGroup(
		@Param("groupId") String groupId,
		@Param("userId") String userId
	);
}
