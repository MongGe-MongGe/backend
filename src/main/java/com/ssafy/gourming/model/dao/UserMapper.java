package com.ssafy.gourming.model.dao;

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
}
