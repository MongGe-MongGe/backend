package com.ssafy.gourming.model.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.mapper.ReviewImpressionMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewImpressionServiceImpl implements ReviewImpressionService {

	private final ReviewImpressionMapper reviewImpressionMapper;

	@Override
	public void record(String userId, List<String> reviewIds) {
		// 같은 요청 안에서 한 리뷰를 두 번 upsert하면 seen_count가 두 번 오르므로 먼저 중복을 없앤다.
		List<String> ids = reviewIds.stream()
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(id -> !id.isEmpty())
			.distinct()
			.toList();
		if (ids.isEmpty()) {
			return;
		}
		reviewImpressionMapper.upsertImpressions(userId, ids);
	}
}
