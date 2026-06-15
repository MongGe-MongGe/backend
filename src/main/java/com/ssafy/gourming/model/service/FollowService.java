package com.ssafy.gourming.model.service;

import java.util.List;
import com.ssafy.gourming.model.dto.UserDto;

public interface FollowService {
    void followUser(String currentUserId, String targetUserId);
    void unfollowUser(String currentUserId, String targetUserId);
    List<UserDto.UserProfileResponse> getFollowings(String userId, String currentUserId);
    List<UserDto.UserProfileResponse> getFollowers(String userId, String currentUserId);
}
