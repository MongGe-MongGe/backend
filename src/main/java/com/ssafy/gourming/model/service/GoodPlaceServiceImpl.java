package com.ssafy.gourming.model.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceCreateRequest;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceDetailResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceEntity;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlacePageResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.mapper.GoodPlaceMapper;
import com.ssafy.gourming.model.mapper.GroupMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoodPlaceServiceImpl implements GoodPlaceService {

	private final GoodPlaceMapper goodPlaceMapper;
	private final GroupMapper groupMapper;
	private final PlaceService placeService;

	@Override
	@Transactional
	public GoodPlaceResponse createGoodPlace(
		String userId,
		String groupId,
		GoodPlaceCreateRequest request
	) {
		GroupEntity group = getGroup(groupId);
		validateOwner(group, userId);

		// 같은 그룹에 이미 저장된 장소라면 중복 생성 없이 기존 결과를 반환한다.
		String placeId = request.getPlace().getId();
		GoodPlaceEntity existingGoodPlace =
			goodPlaceMapper.selectGoodPlace(userId, groupId, placeId);
		if (existingGoodPlace != null) {
			return convertToGoodPlaceResponse(existingGoodPlace);
		}

		// 장소가 DB에 없으면 카카오 장소 검증 후 places 테이블에 저장한다.
		PlaceEntity place = placeService.findOrCreatePlace(request.getPlace());
		if (place == null) {
			throw new NoSuchElementException("Place not found: " + placeId);
		}

		GoodPlaceEntity goodPlace = new GoodPlaceEntity();
		goodPlace.setUserId(userId);
		goodPlace.setGroupId(groupId);
		goodPlace.setPlaceId(place.getId());

		int insertedCount;
		try {
			insertedCount = goodPlaceMapper.insertGoodPlace(goodPlace);
		} catch (DuplicateKeyException exception) {
			// 동시 저장 요청으로 UNIQUE KEY가 충돌하면 먼저 저장된 결과를 반환한다.
			GoodPlaceEntity concurrentlyCreatedGoodPlace =
				goodPlaceMapper.selectGoodPlace(userId, groupId, place.getId());
			if (concurrentlyCreatedGoodPlace != null) {
				return convertToGoodPlaceResponse(concurrentlyCreatedGoodPlace);
			}
			throw exception;
		}
		if (insertedCount == 0) {
			throw new IllegalStateException("Failed to create good place");
		}

		GoodPlaceEntity createdGoodPlace =
			goodPlaceMapper.selectGoodPlace(userId, groupId, place.getId());
		if (createdGoodPlace == null) {
			throw new IllegalStateException("Failed to find created good place");
		}
		return convertToGoodPlaceResponse(createdGoodPlace);
	}

	@Override
	public GoodPlacePageResponse getGroupGoodPlaces(
		String targetUserId,
		String groupId,
		int page,
		int size
	) {
		validatePageRequest(page, size);
		GroupEntity group = getGroup(groupId);

		// 공개 조회에서는 로그인 사용자가 아니라 URL의 사용자와 그룹 소유자를 비교한다.
		if (!group.getUserId().equals(targetUserId)) {
			throw new NoSuchElementException("Group not found: " + groupId);
		}

		long totalElements =
			goodPlaceMapper.countGoodPlacesByGroup(targetUserId, groupId);
		// page는 0부터 시작하므로 앞에서 건너뛸 항목 수를 계산한다.
		long offset = (long)page * size;
		List<GoodPlaceDetailResponse> content =
			goodPlaceMapper.selectGoodPlacesByGroup(
				targetUserId,
				groupId,
				offset,
				size
			);

		return createPageResponse(content, page, size, totalElements);
	}

	@Override
	@Transactional
	public void deleteGoodPlaceFromGroup(String userId, String groupId, String placeId) {
		GroupEntity group = getGroup(groupId);
		validateOwner(group, userId);

		// 다른 그룹의 동일 장소 저장 기록은 유지하고 지정한 그룹의 기록만 삭제한다.
		int deletedCount = goodPlaceMapper.deleteGoodPlaceFromGroup(userId, groupId, placeId);
		if (deletedCount == 0) {
			throw new NoSuchElementException("Good place not found: " + placeId);
		}
	}

	private GroupEntity getGroup(String groupId) {
		GroupEntity group = groupMapper.selectGroupById(groupId);
		if (group == null) {
			throw new NoSuchElementException("Group not found: " + groupId);
		}
		return group;
	}

	private void validateOwner(GroupEntity group, String userId) {
		if (!group.getUserId().equals(userId)) {
			throw new IllegalArgumentException("Group does not belong to user");
		}
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must be zero or greater");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("Size must be between 1 and 100");
		}
	}

	private GoodPlacePageResponse createPageResponse(
		List<GoodPlaceDetailResponse> content,
		int page,
		int size,
		long totalElements
	) {
		int totalPages = (int)((totalElements + size - 1) / size);

		GoodPlacePageResponse response = new GoodPlacePageResponse();
		response.setContent(content);
		response.setPage(page);
		response.setSize(size);
		response.setTotalElements(totalElements);
		response.setTotalPages(totalPages);
		response.setFirst(page == 0);
		response.setLast(totalPages == 0 || page >= totalPages - 1);
		return response;
	}

	private GoodPlaceResponse convertToGoodPlaceResponse(GoodPlaceEntity goodPlace) {
		GoodPlaceResponse response = new GoodPlaceResponse();
		response.setId(goodPlace.getId());
		response.setGroupId(goodPlace.getGroupId());
		response.setPlaceId(goodPlace.getPlaceId());
		response.setCreatedAt(goodPlace.getCreatedAt());
		return response;
	}

	@Override
	public boolean isPlaceSavedByUser(String userId, String placeId) {
		return goodPlaceMapper.isPlaceSavedByUser(userId, placeId);
	}

	@Override
	public java.util.List<String> getGroupIdsByPlace(String userId, String placeId) {
		return goodPlaceMapper.selectGroupIdsByPlace(userId, placeId);
	}
}
