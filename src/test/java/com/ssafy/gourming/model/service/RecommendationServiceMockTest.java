package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.config.RecommendationProperties;
import com.ssafy.gourming.model.client.FakeTextEmbedder;
import com.ssafy.gourming.model.client.TextEmbedder;
import com.ssafy.gourming.model.dto.RecommendationDto.RecommendationCandidateRow;
import com.ssafy.gourming.model.dto.RecommendationDto.RecommendationPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.UserTasteDto.UserVector;
import com.ssafy.gourming.model.mapper.RecommendationMapper;
import com.ssafy.gourming.model.mapper.ReviewMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("추천 피드 서비스 Mock 단위 테스트")
class RecommendationServiceMockTest {

	private static final String VERSION = "test-v1";
	private static final String USER_ID = "user-1";

	@Mock
	private RecommendationMapper recommendationMapper;

	@Mock
	private ReviewMapper reviewMapper;

	@Mock
	private UserTasteService userTasteService;

	@Mock
	private ReviewService reviewService;

	@Mock
	private TextEmbedder textEmbedder;

	private RecommendationProperties properties;
	private RecommendationServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new RecommendationProperties();
		properties.setCandidateSize(500);
		properties.setMaxPerPlace(2);
		service = new RecommendationServiceImpl(
			recommendationMapper, reviewMapper, userTasteService, reviewService, textEmbedder, properties);
	}

	@Test
	@DisplayName("page, size 검증 규칙은 기존 피드와 같다")
	void validatesPageAndSize() {
		assertThatThrownBy(() -> service.getRecommendedReviews(USER_ID, -1, 20, null))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.getRecommendedReviews(USER_ID, 0, 0, null))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.getRecommendedReviews(USER_ID, 0, 101, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("사용자 벡터가 없으면 인기 피드를 그대로 반환한다")
	void fallsBackToPopularWithoutUserVector() {
		when(userTasteService.computeUserVector(eq(USER_ID), any())).thenReturn(null);
		ReviewPageResponse popular = new ReviewPageResponse();
		popular.setContent(List.of());
		when(reviewService.getPopularReviews(USER_ID, 0, 20)).thenReturn(popular);

		RecommendationPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		assertThat(response.getContent()).isSameAs(popular.getContent());
		assertThat(response.getAsOf()).isNotNull();
		verify(recommendationMapper, never()).selectCandidates(any(), any(), anyInt(), any(), any());
	}

	@Test
	@DisplayName("유사도 내림차순으로 정렬하고 Mapper 반환 순서와 무관하게 응답 순서를 유지한다")
	void sortsBySimilarity() {
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of(
			candidate("far", "p1", new float[] {0f, 1f}),
			candidate("near", "p2", new float[] {1f, 0f}),
			candidate("mid", "p3", new float[] {1f, 1f})
		));
		// Mapper는 요청 순서와 반대로 돌려준다.
		when(reviewMapper.selectReviewsByIds(anyList(), eq(USER_ID)))
			.thenAnswer(invocation -> {
				List<String> ids = new ArrayList<>(invocation.<List<String>>getArgument(0));
				Collections.reverse(ids);
				return ids.stream().map(this::review).toList();
			});

		ReviewPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		assertThat(response.getContent()).extracting(ReviewResponse::getId).containsExactly("near", "mid", "far");
		assertThat(response.getTotalElements()).isEqualTo(3);
		assertThat(response.getTotalPages()).isEqualTo(1);
		assertThat(response.isFirst()).isTrue();
		assertThat(response.isLast()).isTrue();
	}

	@Test
	@DisplayName("같은 장소 리뷰는 최대 2개만 포함되고 totalElements는 필터 후 개수다")
	void limitsPerPlace() {
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of(
			candidate("a1", "p1", new float[] {1f, 0f}),
			candidate("a2", "p1", new float[] {0.9f, 0.1f}),
			candidate("a3", "p1", new float[] {0.8f, 0.2f}),
			candidate("b1", "p2", new float[] {0.5f, 0.5f})
		));
		when(reviewMapper.selectReviewsByIds(anyList(), eq(USER_ID)))
			.thenAnswer(invocation -> invocation.<List<String>>getArgument(0).stream().map(this::review).toList());

		ReviewPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		assertThat(response.getContent()).extracting(ReviewResponse::getId).containsExactly("a1", "a2", "b1");
		assertThat(response.getTotalElements()).isEqualTo(3);
	}

	@Test
	@DisplayName("페이지 슬라이스가 겹치지 않고 범위를 넘으면 빈 content를 반환한다")
	void pagesDoNotOverlap() {
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of(
			candidate("r1", "p1", new float[] {1f, 0f}),
			candidate("r2", "p2", new float[] {0.9f, 0.1f}),
			candidate("r3", "p3", new float[] {0.8f, 0.2f})
		));
		when(reviewMapper.selectReviewsByIds(anyList(), eq(USER_ID)))
			.thenAnswer(invocation -> invocation.<List<String>>getArgument(0).stream().map(this::review).toList());

		ReviewPageResponse page0 = service.getRecommendedReviews(USER_ID, 0, 2, null);
		ReviewPageResponse page1 = service.getRecommendedReviews(USER_ID, 1, 2, null);
		ReviewPageResponse page5 = service.getRecommendedReviews(USER_ID, 5, 2, null);

		assertThat(page0.getContent()).extracting(ReviewResponse::getId).containsExactly("r1", "r2");
		assertThat(page1.getContent()).extracting(ReviewResponse::getId).containsExactly("r3");
		assertThat(page0.getTotalPages()).isEqualTo(2);
		assertThat(page0.isLast()).isFalse();
		assertThat(page1.isLast()).isTrue();
		assertThat(page5.getContent()).isEmpty();
		assertThat(page5.getTotalElements()).isEqualTo(3);
		verify(reviewMapper, never()).selectReviewsByIds(eq(List.of()), any());
	}

	@Test
	@DisplayName("후보가 0개이면 빈 페이지를 반환하고 인기 피드로 대체하지 않는다")
	void emptyCandidates() {
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of());

		ReviewPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		assertThat(response.getContent()).isEmpty();
		assertThat(response.getTotalElements()).isZero();
		assertThat(response.getTotalPages()).isZero();
		assertThat(response.isLast()).isTrue();
		verify(reviewService, never()).getPopularReviews(any(), anyInt(), anyInt());
		verify(reviewMapper, never()).selectReviewsByIds(anyList(), any());
	}

	@Test
	@DisplayName("벡터 길이가 다른 후보는 건너뛴다")
	void skipsLengthMismatch() {
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of(
			candidate("ok", "p1", new float[] {1f, 0f}),
			candidate("bad", "p2", new float[] {1f, 0f, 0f})
		));
		when(reviewMapper.selectReviewsByIds(anyList(), eq(USER_ID)))
			.thenAnswer(invocation -> invocation.<List<String>>getArgument(0).stream().map(this::review).toList());

		ReviewPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		assertThat(response.getContent()).extracting(ReviewResponse::getId).containsExactly("ok");
	}

	@Test
	@DisplayName("Fake 임베더 기준 디저트 취향 사용자에게 디저트 리뷰가 매운 음식 리뷰보다 앞선다")
	void dessertLoverSeesDessertFirst() {
		FakeTextEmbedder fake = new FakeTextEmbedder();
		float[] dessertTaste = fake.embed(List.of("케이크와 디저트가 좋아요")).get(0);
		stubUserVector(dessertTaste);
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of(
			candidate("spicy", "p1", fake.embed(List.of("매콤한 떡볶이가 얼큰해요")).get(0)),
			candidate("dessert", "p2", fake.embed(List.of("케이크가 맛있는 디저트 카페")).get(0))
		));
		when(reviewMapper.selectReviewsByIds(anyList(), eq(USER_ID)))
			.thenAnswer(invocation -> invocation.<List<String>>getArgument(0).stream().map(this::review).toList());

		ReviewPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		assertThat(response.getContent()).extracting(ReviewResponse::getId).containsExactly("dessert", "spicy");
	}

	@Test
	@DisplayName("점수와 작성 시각이 같으면 Mapper 반환 순서와 무관하게 리뷰 ID 내림차순으로 정렬한다")
	void breaksTiesByReviewIdDesc() {
		stubUserVector(new float[] {1f, 0f});
		// 점수·작성 시각이 모두 같은 후보를 ID 오름차순으로 돌려준다.
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of(
			candidate("r1", "p1", new float[] {1f, 0f}),
			candidate("r2", "p2", new float[] {1f, 0f}),
			candidate("r3", "p3", new float[] {1f, 0f})
		));
		when(reviewMapper.selectReviewsByIds(anyList(), eq(USER_ID)))
			.thenAnswer(invocation -> invocation.<List<String>>getArgument(0).stream().map(this::review).toList());

		ReviewPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		assertThat(response.getContent()).extracting(ReviewResponse::getId).containsExactly("r3", "r2", "r1");
	}

	@Test
	@DisplayName("asOf가 없으면 현재 시각(초 단위)을 쓰고 사용자 벡터·후보 조회에 같은 값을 넘기며 응답에 담는다")
	void usesNowTruncatedWhenAsOfMissing() {
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of());
		LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

		RecommendationPageResponse response = service.getRecommendedReviews(USER_ID, 0, 20, null);

		LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		LocalDateTime asOf = response.getAsOf();
		assertThat(asOf).isBetween(before, after);
		assertThat(asOf.getNano()).isZero();
		verify(userTasteService).computeUserVector(USER_ID, asOf);
		verify(recommendationMapper).selectCandidates(USER_ID, VERSION, 500, asOf, asOf.minusDays(14));
	}

	@Test
	@DisplayName("받은 asOf를 그대로 쓰고, 미래 asOf는 현재 시각으로 보정한다")
	void usesGivenAsOfAndClampsFuture() {
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of());
		LocalDateTime given = LocalDateTime.of(2026, 9, 30, 11, 25, 52);

		assertThat(service.getRecommendedReviews(USER_ID, 0, 20, given).getAsOf()).isEqualTo(given);
		verify(recommendationMapper).selectCandidates(USER_ID, VERSION, 500, given, given.minusDays(14));

		LocalDateTime future = LocalDateTime.now().plusDays(1);
		LocalDateTime clamped = service.getRecommendedReviews(USER_ID, 0, 20, future).getAsOf();
		assertThat(clamped).isBeforeOrEqualTo(LocalDateTime.now());
		assertThat(clamped.getNano()).isZero();
	}

	@Test
	@DisplayName("seen-exclude-days가 0이면 본 리뷰 제외 기준 시각으로 null을 넘긴다")
	void passesNullSeenSinceWhenDisabled() {
		properties.setSeenExcludeDays(0);
		stubUserVector(new float[] {1f, 0f});
		when(recommendationMapper.selectCandidates(eq(USER_ID), eq(VERSION), eq(500), any(), any())).thenReturn(List.of());

		service.getRecommendedReviews(USER_ID, 0, 20, LocalDateTime.of(2026, 9, 30, 11, 25, 52));

		verify(recommendationMapper).selectCandidates(
			eq(USER_ID), eq(VERSION), eq(500), eq(LocalDateTime.of(2026, 9, 30, 11, 25, 52)), isNull());
	}

	private void stubUserVector(float[] vector) {
		when(textEmbedder.version()).thenReturn(VERSION);
		when(userTasteService.computeUserVector(eq(USER_ID), any())).thenReturn(new UserVector(vector, 3));
	}

	private RecommendationCandidateRow candidate(String reviewId, String placeId, float[] embedding) {
		RecommendationCandidateRow row = new RecommendationCandidateRow();
		row.setReviewId(reviewId);
		row.setPlaceId(placeId);
		row.setEmbedding(embedding);
		row.setCreatedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
		return row;
	}

	private ReviewResponse review(String id) {
		ReviewResponse response = new ReviewResponse();
		response.setId(id);
		return response;
	}
}
