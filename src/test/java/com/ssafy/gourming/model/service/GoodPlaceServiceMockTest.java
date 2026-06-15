package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceCreateRequest;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceDetailResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceEntity;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlacePageResponse;
import com.ssafy.gourming.model.dto.GoodPlaceDto.GoodPlaceResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.mapper.GoodPlaceMapper;
import com.ssafy.gourming.model.mapper.GroupMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("맛집 서비스 Mock 단위 테스트")
class GoodPlaceServiceMockTest {

	private static final String USER_ID = "user-1";
	private static final String OTHER_USER_ID = "user-2";
	private static final String GROUP_ID = "group-1";
	private static final String PLACE_ID = "place-1";

	@Mock
	private GoodPlaceMapper goodPlaceMapper;

	@Mock
	private GroupMapper groupMapper;

	@Mock
	private PlaceService placeService;

	@InjectMocks
	private GoodPlaceServiceImpl goodPlaceService;

	@Test
	@DisplayName("소유한 그룹에 맛집을 저장하고 결과를 반환한다")
	void createGoodPlace() {
		GoodPlaceCreateRequest request = createRequest();
		PlaceEntity place = createPlace();
		GoodPlaceEntity createdGoodPlace = createGoodPlaceEntity();

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(USER_ID));
		when(goodPlaceMapper.selectGoodPlace(USER_ID, GROUP_ID, PLACE_ID))
			.thenReturn(null, createdGoodPlace);
		when(placeService.findOrCreatePlace(request.getPlace())).thenReturn(place);
		when(goodPlaceMapper.insertGoodPlace(any())).thenReturn(1);

		GoodPlaceResponse result =
			goodPlaceService.createGoodPlace(USER_ID, GROUP_ID, request);

		assertThat(result.getId()).isEqualTo("good-place-1");
		assertThat(result.getGroupId()).isEqualTo(GROUP_ID);
		assertThat(result.getPlaceId()).isEqualTo(PLACE_ID);
		assertThat(result.getCreatedAt()).isEqualTo(createdGoodPlace.getCreatedAt());
		verify(goodPlaceMapper).insertGoodPlace(argThat(goodPlace ->
			goodPlace.getUserId().equals(USER_ID)
				&& goodPlace.getGroupId().equals(GROUP_ID)
				&& goodPlace.getPlaceId().equals(PLACE_ID)
		));
	}

	@Test
	@DisplayName("이미 저장된 맛집이면 새로 저장하지 않고 기존 결과를 반환한다")
	void createGoodPlaceReturnsExistingGoodPlace() {
		GoodPlaceCreateRequest request = createRequest();
		GoodPlaceEntity existingGoodPlace = createGoodPlaceEntity();

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(USER_ID));
		when(goodPlaceMapper.selectGoodPlace(USER_ID, GROUP_ID, PLACE_ID))
			.thenReturn(existingGoodPlace);

		GoodPlaceResponse result =
			goodPlaceService.createGoodPlace(USER_ID, GROUP_ID, request);

		assertThat(result.getId()).isEqualTo(existingGoodPlace.getId());
		verify(placeService, never()).findOrCreatePlace(any());
		verify(goodPlaceMapper, never()).insertGoodPlace(any());
	}

	@Test
	@DisplayName("동시 저장으로 중복 키가 발생하면 먼저 저장된 결과를 반환한다")
	void createGoodPlaceReturnsConcurrentlyCreatedGoodPlace() {
		GoodPlaceCreateRequest request = createRequest();
		GoodPlaceEntity createdGoodPlace = createGoodPlaceEntity();

		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(USER_ID));
		when(goodPlaceMapper.selectGoodPlace(USER_ID, GROUP_ID, PLACE_ID))
			.thenReturn(null, createdGoodPlace);
		when(placeService.findOrCreatePlace(request.getPlace())).thenReturn(createPlace());
		when(goodPlaceMapper.insertGoodPlace(any()))
			.thenThrow(new DuplicateKeyException("duplicate"));

		GoodPlaceResponse result =
			goodPlaceService.createGoodPlace(USER_ID, GROUP_ID, request);

		assertThat(result.getId()).isEqualTo(createdGoodPlace.getId());
		assertThat(result.getPlaceId()).isEqualTo(PLACE_ID);
	}

	@Test
	@DisplayName("다른 사용자의 그룹에는 맛집을 저장할 수 없다")
	void createGoodPlaceInOtherUsersGroupFails() {
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(OTHER_USER_ID));

		assertThatThrownBy(() ->
			goodPlaceService.createGoodPlace(USER_ID, GROUP_ID, createRequest()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Group does not belong to user");

		verify(placeService, never()).findOrCreatePlace(any());
		verify(goodPlaceMapper, never()).insertGoodPlace(any());
	}

	@Test
	@DisplayName("존재하지 않는 그룹에는 맛집을 저장할 수 없다")
	void createGoodPlaceInMissingGroupFails() {
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(null);

		assertThatThrownBy(() ->
			goodPlaceService.createGoodPlace(USER_ID, GROUP_ID, createRequest()))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Group not found: " + GROUP_ID);
	}

	@Test
	@DisplayName("장소를 검증할 수 없으면 맛집을 저장하지 않는다")
	void createGoodPlaceWithMissingPlaceFails() {
		GoodPlaceCreateRequest request = createRequest();
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(USER_ID));
		when(goodPlaceMapper.selectGoodPlace(USER_ID, GROUP_ID, PLACE_ID)).thenReturn(null);
		when(placeService.findOrCreatePlace(request.getPlace())).thenReturn(null);

		assertThatThrownBy(() ->
			goodPlaceService.createGoodPlace(USER_ID, GROUP_ID, request))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Place not found: " + PLACE_ID);

		verify(goodPlaceMapper, never()).insertGoodPlace(any());
	}

	@Test
	@DisplayName("맛집 저장이 DB에 반영되지 않으면 실패한다")
	void createGoodPlaceInsertFails() {
		GoodPlaceCreateRequest request = createRequest();
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(USER_ID));
		when(goodPlaceMapper.selectGoodPlace(USER_ID, GROUP_ID, PLACE_ID)).thenReturn(null);
		when(placeService.findOrCreatePlace(request.getPlace())).thenReturn(createPlace());
		when(goodPlaceMapper.insertGoodPlace(any())).thenReturn(0);

		assertThatThrownBy(() ->
			goodPlaceService.createGoodPlace(USER_ID, GROUP_ID, request))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Failed to create good place");
	}

	@Test
	@DisplayName("다른 사용자의 그룹 맛집도 공개 조회할 수 있다")
	void getOtherUsersGroupGoodPlaces() {
		List<GoodPlaceDetailResponse> responses = List.of(new GoodPlaceDetailResponse());
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(OTHER_USER_ID));
		when(goodPlaceMapper.countGoodPlacesByGroup(OTHER_USER_ID, GROUP_ID))
			.thenReturn(21L);
		when(goodPlaceMapper.selectGoodPlacesByGroup(OTHER_USER_ID, GROUP_ID, 20, 10))
			.thenReturn(responses);

		GoodPlacePageResponse result =
			goodPlaceService.getGroupGoodPlaces(OTHER_USER_ID, GROUP_ID, 2, 10);

		assertThat(result.getContent()).isSameAs(responses);
		assertThat(result.getPage()).isEqualTo(2);
		assertThat(result.getSize()).isEqualTo(10);
		assertThat(result.getTotalElements()).isEqualTo(21);
		assertThat(result.getTotalPages()).isEqualTo(3);
		assertThat(result.isFirst()).isFalse();
		assertThat(result.isLast()).isTrue();
		verify(goodPlaceMapper)
			.selectGoodPlacesByGroup(OTHER_USER_ID, GROUP_ID, 20, 10);
	}

	@Test
	@DisplayName("조회 경로의 사용자와 그룹 소유자가 다르면 조회할 수 없다")
	void getGroupGoodPlacesWithWrongUserIdFails() {
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(OTHER_USER_ID));

		assertThatThrownBy(() ->
			goodPlaceService.getGroupGoodPlaces(USER_ID, GROUP_ID, 0, 20))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Group not found: " + GROUP_ID);

		verify(goodPlaceMapper, never())
			.selectGoodPlacesByGroup(any(), any(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("페이지 번호는 0 이상이어야 한다")
	void getGroupGoodPlacesWithNegativePageFails() {
		assertThatThrownBy(() ->
			goodPlaceService.getGroupGoodPlaces(USER_ID, GROUP_ID, -1, 20))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Page must be zero or greater");

		verify(groupMapper, never()).selectGroupById(any());
	}

	@Test
	@DisplayName("페이지 크기는 1 이상 100 이하여야 한다")
	void getGroupGoodPlacesWithInvalidSizeFails() {
		assertThatThrownBy(() ->
			goodPlaceService.getGroupGoodPlaces(USER_ID, GROUP_ID, 0, 101))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Size must be between 1 and 100");

		verify(groupMapper, never()).selectGroupById(any());
	}

	@Test
	@DisplayName("소유한 그룹에서 맛집을 삭제한다")
	void deleteGoodPlaceFromGroup() {
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(USER_ID));
		when(goodPlaceMapper.deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID))
			.thenReturn(1);

		goodPlaceService.deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID);

		verify(goodPlaceMapper, never()).selectGoodPlace(any(), any(), any());
		verify(goodPlaceMapper).deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID);
	}

	@Test
	@DisplayName("다른 사용자의 그룹에서는 맛집을 삭제할 수 없다")
	void deleteGoodPlaceFromOtherUsersGroupFails() {
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(OTHER_USER_ID));

		assertThatThrownBy(() ->
			goodPlaceService.deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Group does not belong to user");

		verify(goodPlaceMapper, never()).deleteGoodPlaceFromGroup(any(), any(), any());
	}

	@Test
	@DisplayName("그룹에 저장되지 않은 맛집은 삭제할 수 없다")
	void deleteMissingGoodPlaceFails() {
		when(groupMapper.selectGroupById(GROUP_ID)).thenReturn(createGroup(USER_ID));
		when(goodPlaceMapper.deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID))
			.thenReturn(0);

		assertThatThrownBy(() ->
			goodPlaceService.deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Good place not found: " + PLACE_ID);

		verify(goodPlaceMapper, never()).selectGoodPlace(any(), any(), any());
		verify(goodPlaceMapper).deleteGoodPlaceFromGroup(USER_ID, GROUP_ID, PLACE_ID);
	}

	private GoodPlaceCreateRequest createRequest() {
		PlaceRequest place = new PlaceRequest();
		place.setId(PLACE_ID);
		place.setName("테스트 맛집");
		place.setX("127.0");
		place.setY("37.0");

		GoodPlaceCreateRequest request = new GoodPlaceCreateRequest();
		request.setPlace(place);
		return request;
	}

	private GroupEntity createGroup(String userId) {
		GroupEntity group = new GroupEntity();
		group.setId(GROUP_ID);
		group.setUserId(userId);
		group.setName("친구 추천");
		return group;
	}

	private PlaceEntity createPlace() {
		PlaceEntity place = new PlaceEntity();
		place.setId(PLACE_ID);
		place.setName("테스트 맛집");
		return place;
	}

	private GoodPlaceEntity createGoodPlaceEntity() {
		GoodPlaceEntity goodPlace = new GoodPlaceEntity();
		goodPlace.setId("good-place-1");
		goodPlace.setUserId(USER_ID);
		goodPlace.setGroupId(GROUP_ID);
		goodPlace.setPlaceId(PLACE_ID);
		goodPlace.setCreatedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
		return goodPlace;
	}
}
