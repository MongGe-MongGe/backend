package com.ssafy.gourming.model.service;

import java.util.List;

import com.ssafy.gourming.model.dto.KakaoPlaceDto;

public interface KakaoMapService {

	List<KakaoPlaceDto> searchPlaceByKeyword(String name, String x, String y);
}
