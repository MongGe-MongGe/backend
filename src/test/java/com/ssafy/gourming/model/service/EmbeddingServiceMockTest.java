package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.config.EmbeddingProperties;
import com.ssafy.gourming.model.client.TextEmbedder;
import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingRefreshResponse;
import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingTargetRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.PlaceEmbeddingRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.ReviewEmbeddingRow;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.mapper.EmbeddingMapper;
import com.ssafy.gourming.model.mapper.PlaceMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("임베딩 서비스 Mock 단위 테스트")
class EmbeddingServiceMockTest {

	private static final String VERSION = "test-v1";

	@Mock
	private EmbeddingMapper embeddingMapper;

	@Mock
	private PlaceMapper placeMapper;

	@Mock
	private TextEmbedder textEmbedder;

	private EmbeddingProperties properties;
	private EmbeddingServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new EmbeddingProperties();
		properties.setBatchSize(2);
		when(textEmbedder.version()).thenReturn(VERSION);
		service = new EmbeddingServiceImpl(embeddingMapper, placeMapper, textEmbedder, properties);
	}

	@Test
	@DisplayName("대상 리뷰를 batchSize 단위로 나눠 임베딩하고 upsert한다")
	void refreshAllChunksReviews() {
		when(embeddingMapper.selectEmbeddingTargets(VERSION))
			.thenReturn(List.of(target("r1", "a"), target("r2", "b"), target("r3", "c")));
		when(textEmbedder.embed(anyList())).thenAnswer(invocation -> {
			List<String> texts = invocation.getArgument(0);
			List<float[]> vectors = new ArrayList<>();
			for (int i = 0; i < texts.size(); i++) {
				vectors.add(new float[] {1f, 0f});
			}
			return vectors;
		});
		when(embeddingMapper.selectPlaceIdsWithEmbeddedReviews(VERSION)).thenReturn(List.of());
		when(embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION)).thenReturn(List.of());

		EmbeddingRefreshResponse response = service.refreshAll();

		verify(textEmbedder, times(2)).embed(anyList());
		verify(embeddingMapper, times(2)).upsertReviewEmbeddings(anyList());
		assertThat(response.getReviewRequestedCount()).isEqualTo(3);
		assertThat(response.getReviewSuccessCount()).isEqualTo(3);
		assertThat(response.getReviewFailedCount()).isZero();
		assertThat(response.getEmbedderVersion()).isEqualTo(VERSION);
		assertThat(response.getStartedAt()).isNotNull();
		assertThat(response.getFinishedAt()).isNotNull();
	}

	@Test
	@DisplayName("한 묶음의 임베딩 실패가 다음 묶음 처리를 막지 않는다")
	void refreshAllIsolatesChunkFailure() {
		when(embeddingMapper.selectEmbeddingTargets(VERSION))
			.thenReturn(List.of(target("r1", "a"), target("r2", "b"), target("r3", "c")));
		when(textEmbedder.embed(anyList()))
			.thenThrow(new IllegalStateException("API down"))
			.thenReturn(List.of(new float[] {1f, 0f}));
		when(embeddingMapper.selectPlaceIdsWithEmbeddedReviews(VERSION)).thenReturn(List.of());
		when(embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION)).thenReturn(List.of());

		EmbeddingRefreshResponse response = service.refreshAll();

		assertThat(response.getReviewRequestedCount()).isEqualTo(3);
		assertThat(response.getReviewFailedCount()).isEqualTo(2);
		assertThat(response.getReviewSuccessCount()).isEqualTo(1);
		verify(embeddingMapper, times(1)).upsertReviewEmbeddings(anyList());
	}

	@Test
	@DisplayName("리뷰가 있는 장소는 리뷰 벡터 평균으로 REVIEWS 소스 저장한다")
	void refreshAllComputesPlaceMean() {
		when(embeddingMapper.selectEmbeddingTargets(VERSION)).thenReturn(List.of());
		when(embeddingMapper.selectPlaceIdsWithEmbeddedReviews(VERSION)).thenReturn(List.of("p1"));
		when(embeddingMapper.selectReviewEmbeddingsByPlaceId("p1", VERSION))
			.thenReturn(List.of(reviewRow(new float[] {1f, 0f}), reviewRow(new float[] {0f, 1f})));
		when(embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION)).thenReturn(List.of());

		EmbeddingRefreshResponse response = service.refreshAll();

		verify(embeddingMapper).upsertPlaceEmbedding(argThat(row ->
			row.getPlaceId().equals("p1")
				&& row.getSource().equals(PlaceEmbeddingRow.SOURCE_REVIEWS)
				&& row.getReviewCount() == 2
				&& Math.abs(row.getEmbedding()[0] - 0.5f) < 1e-6
				&& Math.abs(row.getEmbedding()[1] - 0.5f) < 1e-6
				&& row.getEmbedderVersion().equals(VERSION)
		));
		verify(textEmbedder, never()).embed(anyList());
		assertThat(response.getPlaceRequestedCount()).isEqualTo(1);
		assertThat(response.getPlaceSuccessCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("리뷰가 없는 장소는 카테고리명과 장소명을 임베딩해 CATEGORY 소스로 저장한다")
	void refreshAllEmbedsCategoryForEmptyPlace() {
		when(embeddingMapper.selectEmbeddingTargets(VERSION)).thenReturn(List.of());
		when(embeddingMapper.selectPlaceIdsWithEmbeddedReviews(VERSION)).thenReturn(List.of());
		when(embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION)).thenReturn(List.of("p2"));
		PlaceEntity place = new PlaceEntity();
		place.setId("p2");
		place.setName("강남 디저트바");
		place.setCategoryName("음식점 > 카페 > 디저트카페");
		when(placeMapper.selectPlaceById("p2")).thenReturn(place);
		when(textEmbedder.embed(List.of("음식점 > 카페 > 디저트카페 강남 디저트바")))
			.thenReturn(List.of(new float[] {0f, 1f}));

		EmbeddingRefreshResponse response = service.refreshAll();

		verify(embeddingMapper).upsertPlaceEmbedding(argThat(row ->
			row.getPlaceId().equals("p2")
				&& row.getSource().equals(PlaceEmbeddingRow.SOURCE_CATEGORY)
				&& row.getReviewCount() == 0
		));
		assertThat(response.getPlaceSuccessCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("한 장소의 실패가 다른 장소 처리를 막지 않는다")
	void refreshAllIsolatesPlaceFailure() {
		when(embeddingMapper.selectEmbeddingTargets(VERSION)).thenReturn(List.of());
		when(embeddingMapper.selectPlaceIdsWithEmbeddedReviews(VERSION)).thenReturn(List.of("p1", "p3"));
		when(embeddingMapper.selectReviewEmbeddingsByPlaceId("p1", VERSION))
			.thenThrow(new RuntimeException("DB error"));
		when(embeddingMapper.selectReviewEmbeddingsByPlaceId("p3", VERSION))
			.thenReturn(List.of(reviewRow(new float[] {1f, 1f})));
		when(embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION)).thenReturn(List.of());

		EmbeddingRefreshResponse response = service.refreshAll();

		assertThat(response.getPlaceRequestedCount()).isEqualTo(2);
		assertThat(response.getPlaceFailedCount()).isEqualTo(1);
		assertThat(response.getPlaceSuccessCount()).isEqualTo(1);
		verify(embeddingMapper).upsertPlaceEmbedding(argThat(row -> row.getPlaceId().equals("p3")));
	}

	@Test
	@DisplayName("실행 중에 다시 호출하면 예외를 던진다")
	void refreshAllRejectsConcurrentRun() {
		when(embeddingMapper.selectEmbeddingTargets(VERSION)).thenAnswer(invocation -> {
			assertThat(service.isRunning()).isTrue();
			assertThatThrownBy(() -> service.refreshAll())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("running");
			return List.of();
		});
		when(embeddingMapper.selectPlaceIdsWithEmbeddedReviews(VERSION)).thenReturn(List.of());
		when(embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(VERSION)).thenReturn(List.of());

		service.refreshAll();

		assertThat(service.isRunning()).isFalse();
	}

	private EmbeddingTargetRow target(String reviewId, String content) {
		EmbeddingTargetRow row = new EmbeddingTargetRow();
		row.setReviewId(reviewId);
		row.setContent(content);
		return row;
	}

	private ReviewEmbeddingRow reviewRow(float[] embedding) {
		ReviewEmbeddingRow row = new ReviewEmbeddingRow();
		row.setReviewId("r");
		row.setEmbedding(embedding);
		row.setEmbedderVersion(VERSION);
		return row;
	}
}
