package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.TasteTagDto.TasteTag;

@Mapper
public interface TasteTagMapper {

	// 리뷰 분석기에 전달할 활성 미식 태그 목록을 조회한다.
	List<TasteTag> selectActiveTags();

	// 분석 결과 code를 DB tag id로 매핑하기 위해 태그 목록을 조회한다.
	List<TasteTag> selectTagsByCodes(@Param("codes") List<String> codes);
}
