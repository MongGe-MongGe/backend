package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.PopularFeedDto.PopularReviewScoreRow;
import com.ssafy.gourming.model.mapper.PopularFeedMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopularFeedServiceImpl implements PopularFeedService {

	private final PopularFeedMapper popularFeedMapper;

	@Override
	@Transactional
	public void refreshPopularScores(int windowDays) {
		validateWindowDays(windowDays);

		List<PopularReviewScoreRow> scores = popularFeedMapper.calculatePopularScores(windowDays);
		prepareScores(scores, windowDays);

		popularFeedMapper.deleteScoresByWindowDays(windowDays);
		if (!scores.isEmpty()) {
			popularFeedMapper.insertPopularScores(scores);
		}
	}

	private void validateWindowDays(int windowDays) {
		if (windowDays < 1) {
			throw new IllegalArgumentException("Window days must be positive");
		}
	}

	private void prepareScores(List<PopularReviewScoreRow> scores, int windowDays) {
		LocalDateTime calculatedAt = LocalDateTime.now();
		int rankNo = 1;

		for (PopularReviewScoreRow score : scores) {
			score.setRankNo(rankNo++);
			score.setWindowDays(windowDays);
			score.setCalculatedAt(calculatedAt);
		}
	}
}
