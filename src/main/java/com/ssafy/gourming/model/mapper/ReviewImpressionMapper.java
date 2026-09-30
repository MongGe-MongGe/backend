package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewImpressionMapper {

	// 존재하는 리뷰 중 본인 리뷰가 아닌 것만 기록한다. 이미 본 리뷰는 최근 본 시각과 횟수만 갱신한다.
	int upsertImpressions(
		@Param("userId") String userId,
		@Param("reviewIds") List<String> reviewIds
	);
}
