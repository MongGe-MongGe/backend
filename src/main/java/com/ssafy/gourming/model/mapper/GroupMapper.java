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

	// 회원가입 시 생성되는 사용자 기본 그룹 조회
	GroupEntity selectDefaultGroupByUserId(String userId);

	// 기본 그룹 우선 정렬 및 그룹별 맛집 수 집계
	List<GroupResponse> selectGroupsByUserId(String userId);

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
