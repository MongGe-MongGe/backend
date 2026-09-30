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
		// 공백·null을 거르고 중복을 없애 IN 목록을 줄인다. (SQL은 reviews에서 SELECT하므로 중복이 있어도 한 번만 기록된다.)
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
