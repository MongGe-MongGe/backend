package com.ssafy.gourming.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.ssafy.gourming.model.dto.UserDto;

@Mapper
public interface FollowMapper {
    void insertFollow(@Param("id") String id, @Param("userId") String userId, @Param("targetUserId") String targetUserId);
    void deleteFollow(@Param("userId") String userId, @Param("targetUserId") String targetUserId);
    int checkFollow(@Param("userId") String userId, @Param("targetUserId") String targetUserId);
    List<UserDto.UserProfileResponse> getFollowings(@Param("userId") String userId, @Param("currentUserId") String currentUserId);
    List<UserDto.UserProfileResponse> getFollowers(@Param("userId") String userId, @Param("currentUserId") String currentUserId);
}
