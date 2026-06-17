package com.ssafy.gourming.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class FollowDto {
    /**
     * 팔로우 관계를 데이터베이스의 follows 테이블과 매핑하기 위한 엔티티 클래스입니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowEntity {
        private String id;
        private String userId;
        private String targetUserId;
        private java.util.Date createdAt;
    }
}
