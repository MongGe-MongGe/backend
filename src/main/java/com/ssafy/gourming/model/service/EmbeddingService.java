package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingRefreshResponse;

public interface EmbeddingService {

	// 변경된 리뷰 벡터와 전체 장소 벡터를 다시 계산한다. 실행 중이면 IllegalStateException을 던진다.
	EmbeddingRefreshResponse refreshAll();

	boolean isRunning();
}
