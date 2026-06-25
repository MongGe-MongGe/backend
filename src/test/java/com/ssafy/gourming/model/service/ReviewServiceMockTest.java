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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewCreateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewUpdateRequest;
import com.ssafy.gourming.model.mapper.ReviewMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("리뷰 서비스 Mock 단위 테스트")
class ReviewServiceMockTest {

	private static final String USER_ID = "user-1";
	private static final String OTHER_USER_ID = "user-2";
	private static final String REVIEW_ID = "review-1";
	private static final String PLACE_ID = "place-1";
	private static final int POPULAR_WINDOW_DAYS = 7;

	@Mock
	private ReviewMapper reviewMapper;

	@Mock
	private PlaceService placeService;

	@Mock
	private ImageService imageService;

	@InjectMocks
	private ReviewServiceImpl reviewService;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(reviewService, "popularWindowDays", POPULAR_WINDOW_DAYS);
	}

	@Test
	@DisplayName("리뷰를 생성하고 이미지를 확정한 뒤 생성 결과를 반환한다")
	void createReview() {
		ReviewCreateRequest request = createRequest();
		ReviewResponse createdResponse = createResponse("created-review-id");

		when(placeService.findOrCreatePlace(request.getPlace())).thenReturn(createPlace());
		when(reviewMapper.insertReview(any())).thenReturn(1);
		when(reviewMapper.selectReviewById(any(), any())).thenReturn(createdResponse);

		ReviewResponse result = reviewService.createReview(USER_ID, request);

		assertThat(result).isSameAs(createdResponse);
		verify(reviewMapper).insertReview(argThat(review -> review.getId() != null
				&& !review.getId().isBlank()
				&& review.getUserId().equals(USER_ID)
				&& review.getPlaceId().equals(PLACE_ID)
				&& review.getContent().equals("review content")
				&& review.getImages().equals(List.of("/images/review-1.png"))));
		verify(imageService).confirmImages(new String[] { "/images/review-1.png" });
	}

	@Test
	@DisplayName("장소를 검증할 수 없으면 리뷰를 생성하지 않는다")
	void createReviewWithMissingPlaceFails() {
		ReviewCreateRequest request = createRequest();
		when(placeService.findOrCreatePlace(request.getPlace())).thenReturn(null);

		assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessage("Place not found: " + PLACE_ID);

		verify(reviewMapper, never()).insertReview(any());
		verify(imageService, never()).confirmImages(any());
	}

	@Test
	@DisplayName("리뷰 생성 시 이미지가 null이면 빈 목록으로 저장한다")
	void createReviewWithNullImages() {
		ReviewCreateRequest request = createRequest();
		request.setImages(null);

		when(placeService.findOrCreatePlace(request.getPlace())).thenReturn(createPlace());
		when(reviewMapper.insertReview(any())).thenReturn(1);
		when(reviewMapper.selectReviewById(any(), any())).thenReturn(createResponse(REVIEW_ID));

		reviewService.createReview(USER_ID, request);

		verify(reviewMapper).insertReview(argThat(review -> review.getImages().isEmpty()));
		verify(imageService, never()).confirmImages(any());
	}

	@Test
	@DisplayName("리뷰 상세를 조회한다")
	void getReview() {
		ReviewResponse response = createResponse(REVIEW_ID);
		when(reviewMapper.selectReviewById(REVIEW_ID, OTHER_USER_ID)).thenReturn(response);

		ReviewResponse result = reviewService.getReview(REVIEW_ID, OTHER_USER_ID);

		assertThat(result).isSameAs(response);
	}

	@Test
	@DisplayName("존재하지 않는 리뷰 상세는 조회할 수 없다")
	void getMissingReviewFails() {
		when(reviewMapper.selectReviewById(REVIEW_ID, OTHER_USER_ID)).thenReturn(null);

		assertThatThrownBy(() -> reviewService.getReview(REVIEW_ID, OTHER_USER_ID))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessage("Review not found: " + REVIEW_ID);
	}

	@Test
	@DisplayName("소유한 리뷰를 수정하고 이미지를 동기화한다")
	void updateReview() {
		ReviewUpdateRequest request = createUpdateRequest();
		ReviewEntity existingReview = createReviewEntity(USER_ID);
		ReviewResponse updatedResponse = createResponse(REVIEW_ID);

		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(existingReview);
		when(reviewMapper.updateReview(any())).thenReturn(1);
		when(reviewMapper.selectReviewById(REVIEW_ID, USER_ID)).thenReturn(updatedResponse);

		ReviewResponse result = reviewService.updateReview(USER_ID, REVIEW_ID, request);

		assertThat(result).isSameAs(updatedResponse);
		verify(reviewMapper).updateReview(argThat(review -> review.getId().equals(REVIEW_ID)
				&& review.getUserId().equals(USER_ID)
				&& review.getContent().equals("updated content")
				&& review.getImages().equals(List.of("/images/new.png"))
				&& review.getRatingScore().equals(4)));
		verify(imageService).syncImages(
				new String[] { "/images/old.png" },
				new String[] { "/images/new.png" });
	}

	@Test
	@DisplayName("다른 사용자의 리뷰는 수정할 수 없다")
	void updateOtherUsersReviewFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID))
				.thenReturn(createReviewEntity(OTHER_USER_ID));

		assertThatThrownBy(() -> reviewService.updateReview(USER_ID, REVIEW_ID, createUpdateRequest()))
				.isInstanceOf(SecurityException.class)
				.hasMessage("Review does not belong to user");

		verify(reviewMapper, never()).updateReview(any());
		verify(imageService, never()).syncImages(any(), any());
	}

	@Test
	@DisplayName("소유한 리뷰를 삭제하고 이미지를 삭제한다")
	void deleteReview() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReviewEntity(USER_ID));
		when(reviewMapper.deleteReview(REVIEW_ID, USER_ID)).thenReturn(1);

		reviewService.deleteReview(USER_ID, REVIEW_ID);

		verify(reviewMapper).deleteReview(REVIEW_ID, USER_ID);
		verify(imageService).deleteImages(new String[] { "/images/old.png" });
	}

	@Test
	@DisplayName("다른 사용자의 리뷰는 삭제할 수 없다")
	void deleteOtherUsersReviewFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID))
				.thenReturn(createReviewEntity(OTHER_USER_ID));

		assertThatThrownBy(() -> reviewService.deleteReview(USER_ID, REVIEW_ID))
				.isInstanceOf(SecurityException.class)
				.hasMessage("Review does not belong to user");

		verify(reviewMapper, never()).deleteReview(any(), any());
		verify(imageService, never()).deleteImages(any());
	}

	@Test
	@DisplayName("장소별 리뷰 목록을 페이지 응답으로 반환한다")
	void getReviewsByPlace() {
		List<ReviewResponse> responses = List.of(createResponse("review-1"));
		when(reviewMapper.countReviewsByPlace(PLACE_ID)).thenReturn(21L);
		when(reviewMapper.selectReviewsByPlace(PLACE_ID, OTHER_USER_ID, 20, 10)).thenReturn(responses);

		ReviewPageResponse result = reviewService.getReviewsByPlace(PLACE_ID, OTHER_USER_ID, 2, 10);

		assertThat(result.getContent()).isSameAs(responses);
		assertThat(result.getPage()).isEqualTo(2);
		assertThat(result.getSize()).isEqualTo(10);
		assertThat(result.getTotalElements()).isEqualTo(21);
		assertThat(result.getTotalPages()).isEqualTo(3);
		assertThat(result.isFirst()).isFalse();
		assertThat(result.isLast()).isTrue();
		verify(reviewMapper).selectReviewsByPlace(PLACE_ID, OTHER_USER_ID, 20, 10);
	}

	@Test
	@DisplayName("사용자별 리뷰 목록을 페이지 응답으로 반환한다")
	void getReviewsByUser() {
		List<ReviewResponse> responses = List.of(createResponse("review-1"));
		when(reviewMapper.countReviewsByUser(USER_ID)).thenReturn(1L);
		when(reviewMapper.selectReviewsByUser(USER_ID, OTHER_USER_ID, 0, 20)).thenReturn(responses);

		ReviewPageResponse result = reviewService.getReviewsByUser(USER_ID, OTHER_USER_ID, 0, 20);

		assertThat(result.getContent()).isSameAs(responses);
		assertThat(result.isFirst()).isTrue();
		assertThat(result.isLast()).isTrue();
	}

	@Test
	@DisplayName("MVP 피드는 내가 작성한 리뷰 목록을 사용한다")
	void getMyFeeds() {
		List<ReviewResponse> responses = List.of(createResponse("review-1"));
		when(reviewMapper.countReviewsByUser(USER_ID)).thenReturn(1L);
		when(reviewMapper.selectReviewsByUser(USER_ID, USER_ID, 0, 20)).thenReturn(responses);

		ReviewPageResponse result = reviewService.getMyFeeds(USER_ID, 0, 20);

		assertThat(result.getContent()).isSameAs(responses);
		verify(reviewMapper).countReviewsByUser(USER_ID);
		verify(reviewMapper).selectReviewsByUser(USER_ID, USER_ID, 0, 20);
	}

	@Test
	@DisplayName("인기피드 목록을 페이지 응답으로 반환한다")
	void getPopularReviews() {
		List<ReviewResponse> responses = List.of(createResponse("popular-review-1"));
		when(reviewMapper.countPopularReviews(POPULAR_WINDOW_DAYS)).thenReturn(21L);
		when(reviewMapper.selectPopularReviews(OTHER_USER_ID, POPULAR_WINDOW_DAYS, 20, 10))
				.thenReturn(responses);

		ReviewPageResponse result = reviewService.getPopularReviews(OTHER_USER_ID, 2, 10);

		assertThat(result.getContent()).isSameAs(responses);
		assertThat(result.getPage()).isEqualTo(2);
		assertThat(result.getSize()).isEqualTo(10);
		assertThat(result.getTotalElements()).isEqualTo(21);
		assertThat(result.getTotalPages()).isEqualTo(3);
		assertThat(result.isFirst()).isFalse();
		assertThat(result.isLast()).isTrue();
		verify(reviewMapper).countPopularReviews(POPULAR_WINDOW_DAYS);
		verify(reviewMapper).selectPopularReviews(OTHER_USER_ID, POPULAR_WINDOW_DAYS, 20, 10);
	}

	@Test
	@DisplayName("페이지 번호는 0 이상이어야 한다")
	void getReviewsWithNegativePageFails() {
		assertThatThrownBy(() -> reviewService.getReviewsByPlace(PLACE_ID, OTHER_USER_ID, -1, 20))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Page must be zero or greater");

		verify(reviewMapper, never()).selectReviewsByPlace(any(), any(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("인기피드 페이지 번호는 0 이상이어야 한다")
	void getPopularReviewsWithNegativePageFails() {
		assertThatThrownBy(() -> reviewService.getPopularReviews(OTHER_USER_ID, -1, 20))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Page must be zero or greater");

		verify(reviewMapper, never()).countPopularReviews(anyInt());
		verify(reviewMapper, never()).selectPopularReviews(any(), anyInt(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("페이지 크기는 1 이상 100 이하여야 한다")
	void getReviewsWithInvalidSizeFails() {
		assertThatThrownBy(() -> reviewService.getReviewsByPlace(PLACE_ID, OTHER_USER_ID, 0, 101))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Size must be between 1 and 100");

		verify(reviewMapper, never()).selectReviewsByPlace(any(), any(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("인기피드 페이지 크기는 1 이상 100 이하여야 한다")
	void getPopularReviewsWithInvalidSizeFails() {
		assertThatThrownBy(() -> reviewService.getPopularReviews(OTHER_USER_ID, 0, 101))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Size must be between 1 and 100");

		verify(reviewMapper, never()).countPopularReviews(anyInt());
		verify(reviewMapper, never()).selectPopularReviews(any(), anyInt(), anyLong(), anyInt());
	}

	private ReviewCreateRequest createRequest() {
		PlaceRequest place = new PlaceRequest();
		place.setId(PLACE_ID);
		place.setName("test place");
		place.setX("127.0");
		place.setY("37.0");

		ReviewCreateRequest request = new ReviewCreateRequest();
		request.setPlace(place);
		request.setContent(" review content ");
		request.setImages(List.of("/images/review-1.png"));
		request.setRatingScore(5);
		request.setVisitedAt(LocalDate.of(2026, 6, 18));
		return request;
	}

	private ReviewUpdateRequest createUpdateRequest() {
		ReviewUpdateRequest request = new ReviewUpdateRequest();
		request.setContent(" updated content ");
		request.setImages(List.of("/images/new.png"));
		request.setRatingScore(4);
		request.setVisitedAt(LocalDate.of(2026, 6, 18));
		return request;
	}

	private PlaceEntity createPlace() {
		PlaceEntity place = new PlaceEntity();
		place.setId(PLACE_ID);
		place.setName("test place");
		return place;
	}

	private ReviewEntity createReviewEntity(String userId) {
		ReviewEntity review = new ReviewEntity();
		review.setId(REVIEW_ID);
		review.setUserId(userId);
		review.setPlaceId(PLACE_ID);
		review.setContent("review content");
		review.setImages(List.of("/images/old.png"));
		review.setRatingScore(5);
		review.setVisitedAt(LocalDate.of(2026, 6, 18));
		return review;
	}

	private ReviewResponse createResponse(String reviewId) {
		ReviewResponse response = new ReviewResponse();
		response.setId(reviewId);
		response.setContent("review content");
		response.setImages(List.of("/images/review-1.png"));
		response.setRatingScore(5);
		response.setVisitedAt(LocalDate.of(2026, 6, 18));
		response.setCreatedAt(LocalDateTime.of(2026, 6, 18, 12, 0));
		response.setUpdatedAt(LocalDateTime.of(2026, 6, 18, 12, 0));
		return response;
	}
}
