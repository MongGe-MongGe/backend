package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceReviewSummaryServiceImpl implements PlaceReviewSummaryService {

	private static final String STATUS_COMPLETED = "COMPLETED";
	private static final String STATUS_PROCESSING = "PROCESSING";

	private final PlaceMapper placeMapper;
	private final PlaceReviewSummaryMapper placeReviewSummaryMapper;
	private final PlaceReviewSummarizer placeReviewSummarizer;
	private final PlaceSummaryAiProperties placeSummaryAiProperties;

	@Override
	public PlaceReviewSummaryResponse getOrCreateSummary(String placeId) {
		validatePlaceExists(placeId);

		// 장소 상세 조회에서는 이미 성공적으로 생성된 요약을 우선 사용한다.
		// AI 호출은 느리고 실패할 수 있으므로 매번 재생성하지 않는다.
		PlaceReviewSummaryEntity existingSummary =
			placeReviewSummaryMapper.selectByPlaceId(placeId);
		if (existingSummary != null) {
			if (STATUS_COMPLETED.equals(existingSummary.getStatus())) {
				return toResponse(existingSummary);
			}

			// 다른 요청이 이미 같은 장소의 요약을 생성 중이면 중복 AI 호출을 만들지 않는다.
			// 장소 상세 응답은 유지하고, 프론트에는 요약 없음 상태(null)를 내려준다.
			if (STATUS_PROCESSING.equals(existingSummary.getStatus())) {
				return null;
			}
		}

		try {
			// 저장된 성공 요약이 없으면 최초 조회 시점에 lazy 생성한다.
			return refreshSummary(placeId);
		} catch (RuntimeException exception) {
			// 장소 상세 조회는 요약보다 중요하다.
			// AI 실패가 장소 상세 조회 실패로 전파되지 않도록 null을 반환한다.
			return null;
		}
	}

	@Override
	public PlaceReviewSummaryResponse refreshSummary(String placeId) {
		validatePlaceExists(placeId);

		String modelVersion = getModelVersion();
		// 외부 AI 호출 전에 처리 중 상태를 먼저 남겨 중간 실패 상태를 추적할 수 있게 한다.
		placeReviewSummaryMapper.markProcessing(placeId, modelVersion);

		try {
			// 리뷰 원본은 최신순으로 조회된다.
			// max-review-count 제한은 summarizer 구현체에서 적용한다.
			List<ReviewSummarySourceRow> reviews =
				placeReviewSummaryMapper.selectReviewSourcesByPlaceId(placeId);

			// 실제 AI 구현체 또는 Fake 구현체는 설정값에 따라 Spring이 하나만 주입한다.
			PlaceReviewSummaryGenerateResult generatedSummary =
				placeReviewSummarizer.summarize(placeId, reviews);

			// TypeHandler가 List<String>을 JSON 컬럼으로 변환하므로 서비스에서 직접 직렬화하지 않는다.
			PlaceReviewSummaryEntity entity =
				toEntity(placeId, generatedSummary, modelVersion, STATUS_COMPLETED, null);

			placeReviewSummaryMapper.upsert(entity);

			PlaceReviewSummaryEntity savedSummary =
				placeReviewSummaryMapper.selectByPlaceId(placeId);
			if (savedSummary == null) {
				throw new IllegalStateException("Failed to find saved place review summary: " + placeId);
			}
			return toResponse(savedSummary);
		} catch (RuntimeException exception) {
			// 실패한 장소도 다음 재시도나 관리자 확인이 가능하도록 FAILED 상태와 오류 메시지를 저장한다.
			placeReviewSummaryMapper.markFailed(
				placeId,
				modelVersion,
				truncateErrorMessage(exception.getMessage())
			);
			throw exception;
		}
	}

	@Override
	public PlaceReviewSummaryBulkRefreshResponse refreshAllSummaries() {
		LocalDateTime startedAt = LocalDateTime.now();
		List<String> placeIds = placeMapper.selectAllPlaceIds();
		List<String> failedPlaceIds = new ArrayList<>();
		int successCount = 0;

		// 전체 갱신은 관리자용 작업이다.
		// 한 장소가 실패해도 전체 작업을 중단하지 않고 나머지 장소를 계속 처리한다.
		for (String placeId : placeIds) {
			try {
				refreshSummary(placeId);
				successCount++;
			} catch (RuntimeException exception) {
				failedPlaceIds.add(placeId);
			}
		}

		PlaceReviewSummaryBulkRefreshResponse response =
			new PlaceReviewSummaryBulkRefreshResponse();
		response.setRequestedCount(placeIds.size());
		response.setSuccessCount(successCount);
		response.setFailedCount(failedPlaceIds.size());
		response.setFailedPlaceIds(failedPlaceIds);
		response.setStartedAt(startedAt);
		response.setFinishedAt(LocalDateTime.now());
		return response;
	}

	private void validatePlaceExists(String placeId) {
		PlaceEntity place = placeMapper.selectPlaceById(placeId);
		if (place == null) {
			throw new NoSuchElementException("Place not found: " + placeId);
		}
	}

	private PlaceReviewSummaryEntity toEntity(
		String placeId,
		PlaceReviewSummaryGenerateResult generatedSummary,
		String modelVersion,
		String status,
		String errorMessage
	) {
		PlaceReviewSummaryEntity entity = new PlaceReviewSummaryEntity();
		entity.setPlaceId(placeId);
		entity.setSummary(generatedSummary.getSummary());
		entity.setPositivePoints(normalizeList(generatedSummary.getPositivePoints()));
		entity.setNegativePoints(normalizeList(generatedSummary.getNegativePoints()));
		entity.setRecommendedFor(normalizeList(generatedSummary.getRecommendedFor()));
		entity.setKeywords(normalizeList(generatedSummary.getKeywords()));
		entity.setReviewCount(generatedSummary.getReviewCount());
		entity.setModelVersion(modelVersion);
		entity.setStatus(status);
		entity.setLastReviewUpdatedAt(generatedSummary.getLastReviewUpdatedAt());
		entity.setErrorMessage(errorMessage);
		return entity;
	}

	private PlaceReviewSummaryResponse toResponse(PlaceReviewSummaryEntity entity) {
		PlaceReviewSummaryResponse response = new PlaceReviewSummaryResponse();
		response.setPlaceId(entity.getPlaceId());
		response.setSummary(entity.getSummary());
		response.setPositivePoints(normalizeList(entity.getPositivePoints()));
		response.setNegativePoints(normalizeList(entity.getNegativePoints()));
		response.setRecommendedFor(normalizeList(entity.getRecommendedFor()));
		response.setKeywords(normalizeList(entity.getKeywords()));
		response.setReviewCount(entity.getReviewCount());
		response.setStatus(entity.getStatus());
		response.setLastReviewUpdatedAt(entity.getLastReviewUpdatedAt());
		response.setUpdatedAt(entity.getUpdatedAt());
		return response;
	}

	private List<String> normalizeList(List<String> values) {
		if (values == null) {
			return Collections.emptyList();
		}
		return values;
	}

	private String getModelVersion() {
		// Fake 요약기를 사용하는 환경에서는 실제 모델명이 없으므로 저장값을 fake로 남긴다.
		if (!placeSummaryAiProperties.isEnabled()) {
			return "fake";
		}
		String model = placeSummaryAiProperties.getModel();
		if (model == null || model.isBlank()) {
			return "unknown";
		}
		return model;
	}

	private String truncateErrorMessage(String errorMessage) {
		// DB 컬럼 길이(error_message VARCHAR(500))를 넘지 않도록 방어한다.
		if (errorMessage == null || errorMessage.isBlank()) {
			return "Unknown place review summary error";
		}
		if (errorMessage.length() <= 500) {
			return errorMessage;
		}
		return errorMessage.substring(0, 500);
	}
}
