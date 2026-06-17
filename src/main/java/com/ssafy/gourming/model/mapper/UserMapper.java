package com.ssafy.gourming.model.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.gourming.model.dto.UserDto;

@Mapper
public interface UserMapper {
	
	// 회원가입 시 유저 생성
	void insertUser(UserDto.SignupRequest request);
	
	// 이메일로 사용자 조회
	// 중복 체크 및 로그인 인증을 위해 사용
	UserDto.UserEntity findByEmail(String email);	

	// 사용자 ID로 사용자 조회
	UserDto.UserEntity findById(String id);
	
	// 핸들로 사용자 조회
	UserDto.UserEntity findByHandle(String handle);
	
	// 핸들 존재 여부 확인 (최적화)
	boolean existsByHandle(String handle);
	
	// 프로필 업데이트
	void updateProfile(@org.apache.ibatis.annotations.Param("id") String id, @org.apache.ibatis.annotations.Param("request") UserDto.UpdateProfileRequest request);

	// 유저 검색 (keyword 포함 닉네임/핸들) 및 팔로우 여부 등 통계 반환 (Pagination 포함)
	java.util.List<UserDto.UserProfileResponse> searchUsers(@org.apache.ibatis.annotations.Param("keyword") String keyword, @org.apache.ibatis.annotations.Param("currentUserId") String currentUserId, @org.apache.ibatis.annotations.Param("limit") int limit, @org.apache.ibatis.annotations.Param("offset") int offset);

	// 특정 핸들의 프로필 정보 및 팔로우 통계 조회
	UserDto.UserProfileResponse getUserProfileWithStats(@org.apache.ibatis.annotations.Param("handle") String handle, @org.apache.ibatis.annotations.Param("currentUserId") String currentUserId);
}
