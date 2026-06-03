package com.ssafy.gourming.model.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.gourming.model.dto.PlaceDto;

@Mapper
public interface PlaceMapper {

	PlaceDto selectPlaceById(String id);

	int insertPlace(PlaceDto place);

	int deletePlaceById(String id);
}
