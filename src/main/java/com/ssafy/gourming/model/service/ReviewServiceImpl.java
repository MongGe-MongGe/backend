package com.ssafy.gourming.model.service;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewCreateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewEntity;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewUpdateRequest;
import com.ssafy.gourming.model.mapper.ReviewMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private final ReviewMapper reviewMapper;
	private final PlaceService placeService;
	private final ImageService imageService;

	@Value("${popular-feed.window-days:7}")
	private int popularWindowDays;

	@Override
	@Transactional
	public ReviewResponse createReview(String userId, ReviewCreateRequest request) {
		// 프론트에서 전달한 장소가 실제 카카오 장소인지 검증하고 없으면 저장한다.
		PlaceEntity place = placeService.findOrCreatePlace(request.getPlace());
		if (place == null) {
			throw new NoSuchElementException("Place not found: " + request.getPlace().getId());
		}

		ReviewEntity review = new ReviewEntity();
		review.setId(UUID.randomUUID().toString());
		review.setUserId(userId);
		review.setPlaceId(place.getId());
		review.setContent(normalizeContent(request.getContent()));
		review.setImages(normalizeImages(request.getImages()));
		review.setRatingScore(request.getRatingScore());
		review.setVisitedAt(request.getVisitedAt());

		int insertedCount = reviewMapper.insertReview(review);
		if (insertedCount == 0) {
			throw new IllegalStateException("Failed to create review");
		}

		// 리뷰 저장이 성공한 뒤에만 임시 이미지를 확정 상태로 변경한다.
		confirmImages(review.getImages());

		ReviewResponse response = reviewMapper.selectReviewById(review.getId(), userId);
		if (response == null) {
			throw new IllegalStateException("Failed to find created review");
		}
		return response;
	}

	@Override
	public ReviewResponse getReview(String reviewId, String viewerId) {
		ReviewResponse response = reviewMapper.selectReviewById(reviewId, viewerId);
		if (response == null) {
			throw new NoSuchElementException("Review not found: " + reviewId);
		}
		return response;
	}

	@Override
	@Transactional
	public ReviewResponse updateReview(
		String userId,
		String reviewId,
		ReviewUpdateRequest request
	) {
		ReviewEntity existingReview = getReviewEntity(reviewId);
		validateOwner(existingReview, userId);

		// 이미지 생명주기 처리를 위해 수정 전후 이미지 목록을 비교한다.
		List<String> oldImages = normalizeImages(existingReview.getImages());
		List<String> newImages = normalizeImages(request.getImages());

		ReviewEntity review = new ReviewEntity();
		review.setId(reviewId);
		review.setUserId(userId);
		review.setContent(normalizeContent(request.getContent()));
		review.setImages(newImages);
		review.setRatingScore(request.getRatingScore());
		review.setVisitedAt(request.getVisitedAt());

		int updatedCount = reviewMapper.updateReview(review);
		if (updatedCount == 0) {
			throw new IllegalStateException("Failed to update review: " + reviewId);
		}

		imageService.syncImages(toImageArray(oldImages), toImageArray(newImages));

		ReviewResponse response = reviewMapper.selectReviewById(reviewId, userId);
		if (response == null) {
			throw new NoSuchElementException("Review not found: " + reviewId);
		}
		return response;
	}

	@Override
	@Transactional
	public void deleteReview(String userId, String reviewId) {
		ReviewEntity review = getReviewEntity(reviewId);
		validateOwner(review, userId);

		int deletedCount = reviewMapper.deleteReview(reviewId, userId);
		if (deletedCount == 0) {
			throw new IllegalStateException("Failed to delete review: " + reviewId);
		}

		deleteImages(review.getImages());
	}

	@Override
	public ReviewPageResponse getReviewsByPlace(String placeId, String viewerId, int page, int size) {
		validatePageRequest(page, size);
		long totalElements = reviewMapper.countReviewsByPlace(placeId);
		long offset = (long)page * size;
		List<ReviewResponse> content =
			reviewMapper.selectReviewsByPlace(placeId, viewerId, offset, size);

		return createPageResponse(content, page, size, totalElements);
	}

	@Override
	public ReviewPageResponse getReviewsByUser(String userId, String viewerId, int page, int size) {
		validatePageRequest(page, size);
		long totalElements = reviewMapper.countReviewsByUser(userId);
		long offset = (long)page * size;
		List<ReviewResponse> content =
			reviewMapper.selectReviewsByUser(userId, viewerId, offset, size);

		return createPageResponse(content, page, size, totalElements);
	}

	@Override
	public ReviewPageResponse getMyFeeds(String userId, int page, int size) {
		return getReviewsByUser(userId, userId, page, size);
	}

	@Override
	public ReviewPageResponse getPopularReviews(String viewerId, int page, int size) {
		validatePageRequest(page, size);
		long offset = (long)page * size;
		long totalElements = reviewMapper.countPopularReviews(popularWindowDays);
		List<ReviewResponse> content =
			reviewMapper.selectPopularReviews(viewerId, popularWindowDays, offset, size);

		return createPageResponse(content, page, size, totalElements);
	}

	@Override
	public ReviewPageResponse getAllReviews(String viewerId, int page, int size) {
		validatePageRequest(page, size);
		long offset = (long)page * size;
		
		long totalElements;
		List<ReviewResponse> content;

		if (viewerId != null) {
			// 로그인한 유저: 내 리뷰와 내가 팔로잉한 유저의 피드만 조회
			totalElements = reviewMapper.countFeedsForUser(viewerId);
			content = reviewMapper.selectFeedsForUser(viewerId, viewerId, offset, size);
		} else {
			// 비로그인 유저: 전체 리뷰 조회
			totalElements = reviewMapper.countAllReviews();
			content = reviewMapper.selectAllReviews(viewerId, offset, size);
		}

		return createPageResponse(content, page, size, totalElements);
	}

	private ReviewEntity getReviewEntity(String reviewId) {
		ReviewEntity review = reviewMapper.selectReviewEntityById(reviewId);
		if (review == null) {
			throw new NoSuchElementException("Review not found: " + reviewId);
		}
		return review;
	}

	private void validateOwner(ReviewEntity review, String userId) {
		if (!review.getUserId().equals(userId)) {
			throw new SecurityException("Review does not belong to user");
		}
	}

	private String normalizeContent(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("Review content is required");
		}
		return content.strip();
	}

	private List<String> normalizeImages(List<String> images) {
		if (images == null) {
			return Collections.emptyList();
		}
		return images;
	}

	private void confirmImages(List<String> images) {
		if (!images.isEmpty()) {
			imageService.confirmImages(toImageArray(images));
		}
	}

	private void deleteImages(List<String> images) {
		List<String> normalizedImages = normalizeImages(images);
		if (!normalizedImages.isEmpty()) {
			imageService.deleteImages(toImageArray(normalizedImages));
		}
	}

	private String[] toImageArray(List<String> images) {
		return images.toArray(String[]::new);
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must be zero or greater");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("Size must be between 1 and 100");
		}
	}

	private ReviewPageResponse createPageResponse(
		List<ReviewResponse> content,
		int page,
		int size,
		long totalElements
	) {
		int totalPages = (int)((totalElements + size - 1) / size);

		ReviewPageResponse response = new ReviewPageResponse();
		response.setContent(content);
		response.setPage(page);
		response.setSize(size);
		response.setTotalElements(totalElements);
		response.setTotalPages(totalPages);
		response.setFirst(page == 0);
		response.setLast(totalPages == 0 || page >= totalPages - 1);
		return response;
	}
}
