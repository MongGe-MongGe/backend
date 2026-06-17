package com.ssafy.gourming.model.service;

import org.springframework.stereotype.Service;
import com.ssafy.gourming.model.mapper.FollowMapper;
import com.ssafy.gourming.model.dto.UserDto;
import java.util.List;
import java.util.UUID;

@Service
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;

    public FollowServiceImpl(FollowMapper followMapper) {
        this.followMapper = followMapper;
    }

    @Override
    public void followUser(String currentUserId, String targetUserId) {
        // 1. 자기 자신을 팔로우하는 비정상적인 요청을 차단합니다.
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }
        // 2. 이미 팔로우 상태인지 확인 후, 중복이 아닐 경우에만 새로 삽입합니다.
        if (followMapper.checkFollow(currentUserId, targetUserId) == 0) {
            String newId = UUID.randomUUID().toString();
            followMapper.insertFollow(newId, currentUserId, targetUserId);
        }
    }

    @Override
    public void unfollowUser(String currentUserId, String targetUserId) {
        followMapper.deleteFollow(currentUserId, targetUserId);
    }

    @Override
    public List<UserDto.UserProfileResponse> getFollowings(String userId, String currentUserId) {
        return followMapper.getFollowings(userId, currentUserId);
    }

    @Override
    public List<UserDto.UserProfileResponse> getFollowers(String userId, String currentUserId) {
        return followMapper.getFollowers(userId, currentUserId);
    }
}
