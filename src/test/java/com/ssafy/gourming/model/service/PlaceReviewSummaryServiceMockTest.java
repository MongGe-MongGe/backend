package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

import com.ssafy.gourming.config.PlaceSummaryAiProperties;
import com.ssafy.gourming.model.client.PlaceReviewSummarizer;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryBulkRefreshResponse;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryEntity;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryResponse;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;
import com.ssafy.gourming.model.mapper.PlaceMapper;
import com.ssafy.gourming.model.mapper.PlaceReviewSummaryMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("장소 리뷰 요약 서비스 Mock 단위 테스트")
class PlaceReviewSummaryServiceMockTest {

	private static final String PLACE_ID = "place-summary-service-1";
	private static final String OTHER_PLACE_ID = "place-summary-service-2";

	@Mock
	private PlaceMapper placeMapper;

	@Mock
	private PlaceReviewSummaryMapper placeReviewSummaryMapper;

	@Mock
	private PlaceReviewSummarizer placeReviewSummarizer;

	@Mock
	private PlaceSummaryAiProperties placeSummaryAiProperties;

	@InjectMocks
	private PlaceReviewSummaryServiceImpl placeReviewSummaryService;

	@Test
	@DisplayName("COMPLETED 요약이 있으면 저장된 요약을 반환하고 AI를 호출하지 않는다")
	void getOrCreateSummaryReturnsCompletedSummary() {
		PlaceReviewSummaryEntity completedSummary = createSummaryEntity(
			PLACE_ID,
			"저장된 요약입니다.",
			"COMPLETED"
		);

		when(placeMapper.selectPlaceById(PLACE_ID)).thenReturn(createPlace(PLACE_ID));
		when(placeReviewSummaryMapper.selectByPlaceId(PLACE_ID)).thenReturn(completedSummary);

		PlaceReviewSummaryResponse result =
			placeReviewSummaryService.getOrCreateSummary(PLACE_ID);

		assertThat(result).isNotNull();
		assertThat(result.getPlaceId()).isEqualTo(PLACE_ID);
		assertThat(result.getSummary()).isEqualTo("저장된 요약입니다.");
		assertThat(result.getPositivePoints()).containsExactly("맛이 좋아요");
		assertThat(result.getStatus()).isEqualTo("COMPLETED");
		verify(placeReviewSummarizer, never()).summarize(any(), any());
		verify(placeReviewSummaryMapper, never()).markProcessing(any(), any());
	}

	@Test
	@DisplayName("PROCESSING 요약이 있으면 중복 AI 호출 없이 null을 반환한다")
	void getOrCreateSummaryReturnsNullWhenProcessing() {
		PlaceReviewSummaryEntity processingSummary = createSummaryEntity(
			PLACE_ID,
			null,
			"PROCESSING"
		);

		when(placeMapper.selectPlaceById(PLACE_ID)).thenReturn(createPlace(PLACE_ID));
		when(placeReviewSummaryMapper.selectByPlaceId(PLACE_ID)).thenReturn(processingSummary);

		PlaceReviewSummaryResponse result =
			placeReviewSummaryService.getOrCreateSummary(PLACE_ID);

		assertThat(result).isNull();
		verify(placeReviewSummarizer, never()).summarize(any(), any());
		verify(placeReviewSummaryMapper, never()).markProcessing(any(), any());
	}

	@Test
	@DisplayName("저장된 요약이 없으면 리뷰 기준으로 요약을 생성하고 저장한다")
	void getOrCreateSummaryCreatesSummary() {
		PlaceReviewSummaryGenerateResult generatedSummary = createGeneratedSummary();
		PlaceReviewSummaryEntity savedSummary = createSummaryEntity(
			PLACE_ID,
			generatedSummary.getSummary(),
			"COMPLETED"
		);

		when(placeMapper.selectPlaceById(PLACE_ID)).thenReturn(createPlace(PLACE_ID));
		when(placeReviewSummaryMapper.selectByPlaceId(PLACE_ID))
			.thenReturn(null, savedSummary);
		when(placeReviewSummaryMapper.selectReviewSourcesByPlaceId(PLACE_ID))
			.thenReturn(List.of(createReviewSource()));
		when(placeReviewSummarizer.summarize(eq(PLACE_ID), any()))
			.thenReturn(generatedSummary);

		PlaceReviewSummaryResponse result =
			placeReviewSummaryService.getOrCreateSummary(PLACE_ID);

		assertThat(result).isNotNull();
		assertThat(result.getSummary()).isEqualTo(generatedSummary.getSummary());
		assertThat(result.getReviewCount()).isEqualTo(1);
		verify(placeReviewSummaryMapper).markProcessing(PLACE_ID, "fake");
		verify(placeReviewSummaryMapper).upsert(argThat(summary ->
			summary.getPlaceId().equals(PLACE_ID)
				&& summary.getStatus().equals("COMPLETED")
				&& summary.getModelVersion().equals("fake")
				&& summary.getPositivePoints().contains("맛이 좋아요")
		));
	}

	@Test
	@DisplayName("getOrCreateSummary에서 요약 생성이 실패하면 FAILED 저장 후 null을 반환한다")
	void getOrCreateSummaryReturnsNullWhenSummaryCreationFails() {
		when(placeMapper.selectPlaceById(PLACE_ID)).thenReturn(createPlace(PLACE_ID));
		when(placeReviewSummaryMapper.selectByPlaceId(PLACE_ID)).thenReturn(null);
		when(placeReviewSummaryMapper.selectReviewSourcesByPlaceId(PLACE_ID))
			.thenReturn(List.of(createReviewSource()));
		when(placeReviewSummarizer.summarize(eq(PLACE_ID), any()))
			.thenThrow(new IllegalStateException("AI failed"));

		PlaceReviewSummaryResponse result =
			placeReviewSummaryService.getOrCreateSummary(PLACE_ID);

		assertThat(result).isNull();
		verify(placeReviewSummaryMapper).markProcessing(PLACE_ID, "fake");
		verify(placeReviewSummaryMapper).markFailed(
			PLACE_ID,
			"fake",
			"AI failed"
		);
	}

	@Test
	@DisplayName("refreshSummary에서 요약 생성이 실패하면 FAILED 저장 후 예외를 전파한다")
	void refreshSummaryThrowsWhenSummaryCreationFails() {
		when(placeMapper.selectPlaceById(PLACE_ID)).thenReturn(createPlace(PLACE_ID));
		when(placeReviewSummaryMapper.selectReviewSourcesByPlaceId(PLACE_ID))
			.thenReturn(List.of(createReviewSource()));
		when(placeReviewSummarizer.summarize(eq(PLACE_ID), any()))
			.thenThrow(new IllegalStateException("AI failed"));

		assertThatThrownBy(() -> placeReviewSummaryService.refreshSummary(PLACE_ID))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("AI failed");

		verify(placeReviewSummaryMapper).markProcessing(PLACE_ID, "fake");
		verify(placeReviewSummaryMapper).markFailed(
			PLACE_ID,
			"fake",
			"AI failed"
		);
	}

	@Test
	@DisplayName("refreshSummary는 존재하지 않는 장소를 갱신할 수 없다")
	void refreshSummaryWithMissingPlaceFails() {
		when(placeMapper.selectPlaceById(PLACE_ID)).thenReturn(null);

		assertThatThrownBy(() -> placeReviewSummaryService.refreshSummary(PLACE_ID))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Place not found: " + PLACE_ID);

		verify(placeReviewSummaryMapper, never()).markProcessing(any(), any());
		verify(placeReviewSummarizer, never()).summarize(any(), any());
	}

	@Test
	@DisplayName("전체 요약 갱신은 일부 장소가 실패해도 다음 장소를 계속 처리한다")
	void refreshAllSummariesContinuesWhenSomePlaceFails() {
		PlaceReviewSummaryGenerateResult generatedSummary = createGeneratedSummary();
		PlaceReviewSummaryEntity savedSummary = createSummaryEntity(
			PLACE_ID,
			generatedSummary.getSummary(),
			"COMPLETED"
		);

		when(placeMapper.selectAllPlaceIds())
			.thenReturn(List.of(PLACE_ID, OTHER_PLACE_ID));
		when(placeMapper.selectPlaceById(PLACE_ID)).thenReturn(createPlace(PLACE_ID));
		when(placeMapper.selectPlaceById(OTHER_PLACE_ID)).thenReturn(createPlace(OTHER_PLACE_ID));
		when(placeReviewSummaryMapper.selectReviewSourcesByPlaceId(PLACE_ID))
			.thenReturn(List.of(createReviewSource()));
		when(placeReviewSummaryMapper.selectReviewSourcesByPlaceId(OTHER_PLACE_ID))
			.thenReturn(List.of(createReviewSource()));
		when(placeReviewSummarizer.summarize(eq(PLACE_ID), any()))
			.thenReturn(generatedSummary);
		when(placeReviewSummarizer.summarize(eq(OTHER_PLACE_ID), any()))
			.thenThrow(new IllegalStateException("AI failed"));
		when(placeReviewSummaryMapper.selectByPlaceId(PLACE_ID))
			.thenReturn(savedSummary);

		PlaceReviewSummaryBulkRefreshResponse result =
			placeReviewSummaryService.refreshAllSummaries();

		assertThat(result.getRequestedCount()).isEqualTo(2);
		assertThat(result.getSuccessCount()).isEqualTo(1);
		assertThat(result.getFailedCount()).isEqualTo(1);
		assertThat(result.getFailedPlaceIds()).containsExactly(OTHER_PLACE_ID);
		assertThat(result.getStartedAt()).isNotNull();
		assertThat(result.getFinishedAt()).isNotNull();
		verify(placeReviewSummaryMapper).markFailed(
			OTHER_PLACE_ID,
			"fake",
			"AI failed"
		);
	}

	private PlaceEntity createPlace(String placeId) {
		PlaceEntity place = new PlaceEntity();
		place.setId(placeId);
		place.setName("테스트 장소");
		place.setCategoryName("음식점 > 양식");
		place.setCategoryGroupCode("FD6");
		place.setRoadAddressName("서울시 테스트로 1");
		place.setX("127.000000");
		place.setY("37.000000");
		return place;
	}

	private PlaceReviewSummaryEntity createSummaryEntity(
		String placeId,
		String summaryText,
		String status
	) {
		PlaceReviewSummaryEntity summary = new PlaceReviewSummaryEntity();
		summary.setPlaceId(placeId);
		summary.setSummary(summaryText);
		summary.setPositivePoints(List.of("맛이 좋아요"));
		summary.setNegativePoints(List.of("대기가 길 수 있어요"));
		summary.setRecommendedFor(List.of("데이트"));
		summary.setKeywords(List.of("파스타", "분위기"));
		summary.setReviewCount(1);
		summary.setModelVersion("fake");
		summary.setStatus(status);
		summary.setLastReviewUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 0));
		summary.setUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 10));
		return summary;
	}

	private PlaceReviewSummaryGenerateResult createGeneratedSummary() {
		PlaceReviewSummaryGenerateResult result = new PlaceReviewSummaryGenerateResult();
		result.setSummary("생성된 요약입니다.");
		result.setPositivePoints(List.of("맛이 좋아요"));
		result.setNegativePoints(List.of());
		result.setRecommendedFor(List.of("데이트"));
		result.setKeywords(List.of("파스타"));
		result.setReviewCount(1);
		result.setLastReviewUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 0));
		return result;
	}

	private ReviewSummarySourceRow createReviewSource() {
		ReviewSummarySourceRow review = new ReviewSummarySourceRow();
		review.setReviewId("review-1");
		review.setContent("맛있고 분위기가 좋아요.");
		review.setRatingScore(5);
		review.setVisitedAt(LocalDate.of(2026, 6, 24));
		review.setCreatedAt(LocalDateTime.of(2026, 6, 24, 11, 0));
		review.setUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 0));
		return review;
	}
}
