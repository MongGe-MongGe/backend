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
	
	// 핸들로 사용자 조회
	UserDto.UserEntity findByHandle(String handle);
	
	// 핸들 존재 여부 확인 (최적화)
	boolean existsByHandle(String handle);
	
	// 프로필 업데이트
	void updateProfile(@org.apache.ibatis.annotations.Param("id") String id, @org.apache.ibatis.annotations.Param("request") UserDto.UpdateProfileRequest request);
}
