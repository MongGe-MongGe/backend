package com.ssafy.gourming.model.client;

import java.util.List;

import com.ssafy.gourming.model.dto.KakaoPlaceDto;

public interface KakaoLocalClient {

	List<KakaoPlaceDto> searchPlaceByKeyword(String name, String x, String y);
}
