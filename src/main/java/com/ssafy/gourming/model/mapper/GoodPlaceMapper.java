package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceDetailResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceEntity;

@Mapper
public interface GoodPlaceMapper {

	int insertGoodPlace(GoodPlaceEntity goodPlace);

	GoodPlaceEntity selectGoodPlace(
		@Param("userId") String userId,
		@Param("groupId") String groupId,
		@Param("placeId") String placeId
	);

	List<GoodPlaceDetailResponse> selectGoodPlacesByGroup(
		@Param("userId") String userId,
		@Param("groupId") String groupId,
		@Param("offset") long offset,
		@Param("size") int size
	);

	long countGoodPlacesByGroup(
		@Param("userId") String userId,
		@Param("groupId") String groupId
	);

	int deleteGoodPlaceFromGroup(
		@Param("userId") String userId,
		@Param("groupId") String groupId,
		@Param("placeId") String placeId
	);
	
	boolean isPlaceSavedByUser(
		@Param("userId") String userId,
		@Param("placeId") String placeId
	);

	List<String> selectGroupIdsByPlace(
		@Param("userId") String userId,
		@Param("placeId") String placeId
	);
}
