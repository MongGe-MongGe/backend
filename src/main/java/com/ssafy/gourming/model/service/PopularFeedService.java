package com.ssafy.gourming.model.service;

public interface PopularFeedService {

	// 인기피드 점수를 재계산하고 집계 결과를 갱신한다.
	void refreshPopularScores(int windowDays);
}
