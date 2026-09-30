package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;

import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteProfileResponse;
import com.ssafy.gourming.model.dto.UserTasteDto.UserVector;

public interface UserTasteService {

	// 저장 장소·좋아요·별점 행동의 가중 평균 벡터를 계산한다. 행동이 없거나 가중치 합이 0 이하이면 null.
	UserVector computeUserVector(String userId);

	// asOf 이전 행동만으로 계산한다. asOf가 null이면 computeUserVector(userId)와 같다.
	UserVector computeUserVector(String userId, LocalDateTime asOf);

	UserTasteProfileResponse getTasteProfile(String userId);
}
