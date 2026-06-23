package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.PopularFeedDto.PopularReviewScoreRow;

@Mapper
public interface PopularFeedMapper {

	List<PopularReviewScoreRow> calculatePopularScores(@Param("windowDays") int windowDays);

	void deleteScoresByWindowDays(@Param("windowDays") int windowDays);

	int insertPopularScores(@Param("scores") List<PopularReviewScoreRow> scores);
}
