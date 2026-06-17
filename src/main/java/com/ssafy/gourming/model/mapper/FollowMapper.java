package com.ssafy.gourming.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.ssafy.gourming.model.dto.UserDto;

@Mapper
public interface FollowMapper {
    /**
     * 두 사용자 간의 팔로우 관계를 데이터베이스에 삽입합니다.
     * 
     * @param id 팔로우 관계 레코드의 고유 UUID
     * @param userId 팔로우를 요청한 사용자 ID
     * @param targetUserId 팔로우의 대상이 되는 사용자 ID
     */
    void insertFollow(@Param("id") String id, @Param("userId") String userId, @Param("targetUserId") String targetUserId);

    void deleteFollow(@Param("userId") String userId, @Param("targetUserId") String targetUserId);

    /**
     * 이미 팔로우 상태인지 확인합니다.
     * 
     * @param userId 팔로우 요청 사용자 ID
     * @param targetUserId 팔로우 대상 사용자 ID
     * @return 레코드 개수 (1이면 이미 팔로우 중, 0이면 아님)
     */
    int checkFollow(@Param("userId") String userId, @Param("targetUserId") String targetUserId);

    /**
     * 특정 사용자가 팔로우하고 있는 사용자(팔로잉) 목록과 상호 팔로우 상태를 조회합니다.
     * 
     * @param userId 조회 대상 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID (팔로우 여부 파악용)
     * @return 팔로잉 유저 목록
     */
    List<UserDto.UserProfileResponse> getFollowings(@Param("userId") String userId, @Param("currentUserId") String currentUserId);

    /**
     * 특정 사용자를 팔로우하는 사용자(팔로워) 목록과 상호 팔로우 상태를 조회합니다.
     * 
     * @param userId 조회 대상 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID (팔로우 여부 파악용)
     * @return 팔로워 유저 목록
     */
    List<UserDto.UserProfileResponse> getFollowers(@Param("userId") String userId, @Param("currentUserId") String currentUserId);
}
