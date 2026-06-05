package com.ssafy.gourming.model.client;

import java.util.List;

import com.ssafy.gourming.model.dto.KakaoPlaceDto;

public interface KakaoLocalClient {

	// 장소명과 좌표를 기준으로 가까운 카카오 장소를 검색한다.
	List<KakaoPlaceDto> searchPlaceByKeyword(String name, String x, String y);
}
