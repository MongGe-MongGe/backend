package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.TasteTagDto.TasteTagRow;

@Mapper
public interface TasteTagMapper {

	// 활성 태그를 정렬 순서대로 조회한다. 벡터가 아직 없는 태그도 포함한다.
	List<TasteTagRow> selectActiveTags();

	// 활성 태그 중 현재 버전 벡터가 없는 태그를 조회한다.
	List<TasteTagRow> selectTagsMissingVersion(@Param("embedderVersion") String embedderVersion);

	int upsertTagEmbedding(
		@Param("code") String code,
		@Param("embedding") float[] embedding,
		@Param("embedderVersion") String embedderVersion
	);
}
