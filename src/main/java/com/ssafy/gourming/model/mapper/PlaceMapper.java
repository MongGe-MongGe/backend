package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;

@Mapper
public interface PlaceMapper {

	PlaceEntity selectPlaceById(String id);

	int insertPlace(PlaceEntity place);

	int deletePlaceById(String id);

	List<String> selectAllPlaceIds();
}
