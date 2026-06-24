package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupWithUserResponse;

@Mapper
public interface GroupMapper {

	int insertGroup(GroupEntity group);

	GroupEntity selectGroupById(String groupId);

	// 회원가입 시 생성되는 사용자 기본 그룹 조회
	GroupEntity selectDefaultGroupByUserId(String userId);

	// 기본 그룹 우선 정렬 및 그룹별 맛집 수 집계
	List<GroupResponse> selectGroupsByUserId(String userId);

	/**
	 * 본인 그룹과 팔로우하는 유저들의 그룹을 조회합니다.
	 * 맛집이 0개인 그룹은 제외되며, 기본 그룹 우선 및 생성일 순으로 정렬됩니다.
	 * 
	 * @param userId 조회할 기준 유저 ID
	 * @return 그룹 및 해당 그룹 소유자 정보를 포함한 리스트
	 */
	List<GroupWithUserResponse> selectFollowingGroups(String userId);

	// 수정된 그룹의 맛집 수를 포함한 응답 단건 조회
	GroupResponse selectGroupResponseById(
		@Param("userId") String userId,
		@Param("groupId") String groupId
	);

	// 그룹 생성·수정 전 사용자별 이름 중복 확인
	GroupEntity selectGroupByUserIdAndName(
		@Param("userId") String userId,
		@Param("name") String name
	);

	// userId 조건을 포함해 다른 사용자의 그룹 변경 방지
	int updateGroupName(
		@Param("groupId") String groupId,
		@Param("userId") String userId,
		@Param("name") String name
	);

	// userId 조건을 포함해 다른 사용자의 그룹 삭제 방지
	int deleteGroup(
		@Param("groupId") String groupId,
		@Param("userId") String userId
	);
}
