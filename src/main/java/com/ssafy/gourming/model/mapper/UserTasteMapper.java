package com.ssafy.gourming.model.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteEvidenceRow;

@Mapper
public interface UserTasteMapper {

	// 저장 장소(+5), 좋아요 리뷰(+3), 내 리뷰 별점(+4/+2/-2/-4)을 벡터·가중치·시각과 함께 조회한다.
	// 현재 버전 벡터만 포함하며, 별점 3점과 별점 없는 리뷰는 제외한다.
	// asOf가 있으면 그 시각 이전의 행동만 조회한다. 추천 피드 페이지 사이 결과를 고정하는 데 쓴다.
	List<UserTasteEvidenceRow> selectEvidences(
		@Param("userId") String userId,
		@Param("embedderVersion") String embedderVersion,
		@Param("asOf") LocalDateTime asOf
	);
}
