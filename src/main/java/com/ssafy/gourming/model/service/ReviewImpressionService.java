package com.ssafy.gourming.model.service;

import java.util.List;

public interface ReviewImpressionService {

	// 사용자가 피드에서 실제로 본 리뷰를 기록한다. 존재하지 않는 리뷰와 본인 리뷰는 조용히 무시한다.
	void record(String userId, List<String> reviewIds);
}
