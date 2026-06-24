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

	// 비밀번호 재설정 확정 시 암호화된 새 비밀번호로 변경
	int updatePassword(@org.apache.ibatis.annotations.Param("id") String id, @org.apache.ibatis.annotations.Param("encodedPassword") String encodedPassword);

	/**
	 * 키워드가 포함된 닉네임이나 핸들을 가진 사용자를 검색합니다.
	 * 
	 * @param keyword 검색어
	 * @param currentUserId 현재 요청을 보낸 사용자의 ID (팔로우 상태 조회를 위해 사용)
	 * @param limit 조회할 최대 개수 (페이지네이션)
	 * @param offset 조회를 시작할 오프셋 (페이지네이션)
	 * @return 조건에 맞는 사용자 프로필 목록
	 */
	java.util.List<UserDto.UserProfileResponse> searchUsers(@org.apache.ibatis.annotations.Param("keyword") String keyword, @org.apache.ibatis.annotations.Param("currentUserId") String currentUserId, @org.apache.ibatis.annotations.Param("limit") int limit, @org.apache.ibatis.annotations.Param("offset") int offset);

	/**
	 * 특정 핸들을 기반으로 사용자의 프로필 상세 정보(팔로우/팔로워 수 및 팔로우 상태 포함)를 조회합니다.
	 * 
	 * @param handle 조회 대상의 고유 핸들
	 * @param currentUserId 현재 요청을 보낸 사용자의 ID (팔로우 상태 조회를 위해 사용)
	 * @return 대상 사용자의 프로필 응답 객체
	 */
	UserDto.UserProfileResponse getUserProfileWithStats(@org.apache.ibatis.annotations.Param("handle") String handle, @org.apache.ibatis.annotations.Param("currentUserId") String currentUserId);
}
