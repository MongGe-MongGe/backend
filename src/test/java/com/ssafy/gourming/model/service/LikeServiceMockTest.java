package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.dto.LikeDto.LikeResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.mapper.LikeMapper;
import com.ssafy.gourming.model.mapper.ReviewMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("좋아요 서비스 Mock 단위 테스트")
class LikeServiceMockTest {

	private static final String USER_ID = "user-1";
	private static final String REVIEW_ID = "review-1";

	@Mock
	private LikeMapper likeMapper;

	@Mock
	private ReviewMapper reviewMapper;

	@InjectMocks
	private LikeServiceImpl likeService;

	@Test
	@DisplayName("리뷰 좋아요를 등록하고 현재 좋아요 상태를 반환한다")
	void likeReview() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());
		when(likeMapper.insertLike(USER_ID, REVIEW_ID)).thenReturn(1);
		when(likeMapper.countLikesByReview(REVIEW_ID)).thenReturn(1L);

		LikeResponse result = likeService.likeReview(USER_ID, REVIEW_ID);

		assertThat(result.getReviewId()).isEqualTo(REVIEW_ID);
		assertThat(result.isLikedByMe()).isTrue();
		assertThat(result.getLikeCount()).isEqualTo(1);
		verify(likeMapper).insertLike(USER_ID, REVIEW_ID);
	}

	@Test
	@DisplayName("이미 좋아요한 리뷰는 중복 등록 없이 현재 좋아요 상태를 반환한다")
	void likeAlreadyLikedReview() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());
		when(likeMapper.insertLike(USER_ID, REVIEW_ID)).thenReturn(0);
		when(likeMapper.countLikesByReview(REVIEW_ID)).thenReturn(1L);

		LikeResponse result = likeService.likeReview(USER_ID, REVIEW_ID);

		assertThat(result.isLikedByMe()).isTrue();
		assertThat(result.getLikeCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("존재하지 않는 리뷰에는 좋아요를 등록할 수 없다")
	void likeMissingReviewFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(null);

		assertThatThrownBy(() -> likeService.likeReview(USER_ID, REVIEW_ID))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Review not found: " + REVIEW_ID);

		verify(likeMapper, never()).insertLike(USER_ID, REVIEW_ID);
		verify(likeMapper, never()).countLikesByReview(REVIEW_ID);
	}

	@Test
	@DisplayName("리뷰 좋아요를 취소하고 현재 좋아요 상태를 반환한다")
	void unlikeReview() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());
		when(likeMapper.deleteLike(USER_ID, REVIEW_ID)).thenReturn(1);
		when(likeMapper.countLikesByReview(REVIEW_ID)).thenReturn(0L);

		LikeResponse result = likeService.unlikeReview(USER_ID, REVIEW_ID);

		assertThat(result.getReviewId()).isEqualTo(REVIEW_ID);
		assertThat(result.isLikedByMe()).isFalse();
		assertThat(result.getLikeCount()).isZero();
		verify(likeMapper).deleteLike(USER_ID, REVIEW_ID);
	}

	@Test
	@DisplayName("좋아요하지 않은 리뷰 취소는 현재 취소 상태를 반환한다")
	void unlikeMissingLike() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(createReview());
		when(likeMapper.deleteLike(USER_ID, REVIEW_ID)).thenReturn(0);
		when(likeMapper.countLikesByReview(REVIEW_ID)).thenReturn(0L);

		LikeResponse result = likeService.unlikeReview(USER_ID, REVIEW_ID);

		assertThat(result.isLikedByMe()).isFalse();
		assertThat(result.getLikeCount()).isZero();
	}

	@Test
	@DisplayName("존재하지 않는 리뷰의 좋아요는 취소할 수 없다")
	void unlikeMissingReviewFails() {
		when(reviewMapper.selectReviewEntityById(REVIEW_ID)).thenReturn(null);

		assertThatThrownBy(() -> likeService.unlikeReview(USER_ID, REVIEW_ID))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Review not found: " + REVIEW_ID);

		verify(likeMapper, never()).deleteLike(USER_ID, REVIEW_ID);
		verify(likeMapper, never()).countLikesByReview(REVIEW_ID);
	}

	private ReviewEntity createReview() {
		ReviewEntity review = new ReviewEntity();
		review.setId(REVIEW_ID);
		return review;
	}
}
