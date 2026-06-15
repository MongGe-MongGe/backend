package com.ssafy.gourming.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class FollowDto {
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
