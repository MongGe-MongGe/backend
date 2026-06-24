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

	private final PlaceMapper placeMapper;
	private final PlaceReviewSummaryMapper placeReviewSummaryMapper;
	private final PlaceReviewSummarizer placeReviewSummarizer;
	private final PlaceSummaryAiProperties placeSummaryAiProperties;

	@Override
	public PlaceReviewSummaryResponse getSummary(String placeId) {
		// 공개 API에서는 저장된 요약만 읽는다.
		// 요약 생성은 비용과 지연이 큰 작업이므로 관리자 갱신 API에서만 수행한다.
		PlaceReviewSummaryEntity existingSummary =
			placeReviewSummaryMapper.selectByPlaceId(placeId);

		if (existingSummary == null) {
			return null;
		}
		// PROCESSING/FAILED 상태는 사용자에게 노출하지 않고 요약 없음으로 취급한다.
		// FAILED 재시도 역시 공개 API가 아니라 관리자 PUT 요청으로만 수행한다.
		if (!STATUS_COMPLETED.equals(existingSummary.getStatus())) {
			return null;
		}
		return toResponse(existingSummary);
	}

	@Override
	public PlaceReviewSummaryResponse refreshSummary(String placeId) {
		validatePlaceExists(placeId);

		String modelVersion = getModelVersion();
		// 관리자 갱신 요청에서만 AI 요약 생성을 수행한다.
		// 먼저 PROCESSING으로 표시해 두면 갱신 중인 상태를 DB에서 확인할 수 있다.
		placeReviewSummaryMapper.markProcessing(placeId, modelVersion);

		try {
			// AI 입력으로 사용할 리뷰 원문을 최신순으로 조회한다.
			List<ReviewSummarySourceRow> reviews =
				placeReviewSummaryMapper.selectReviewSourcesByPlaceId(placeId);

			// 설정에 따라 실제 AI 생성기 또는 Fake 생성기가 주입된다.
			// 리뷰가 없는 경우에도 생성기가 COMPLETED로 저장 가능한 빈 요약을 반환한다.
			PlaceReviewSummaryGenerateResult generatedSummary =
				placeReviewSummarizer.summarize(placeId, reviews);

			PlaceReviewSummaryEntity entity =
				toEntity(placeId, generatedSummary, modelVersion, STATUS_COMPLETED, null);

			// 기존 요약이 있으면 덮어쓰고, 없으면 새로 저장한다.
			placeReviewSummaryMapper.upsert(entity);

			// DB에 저장된 값을 다시 읽어서 응답 DTO와 실제 저장 상태를 맞춘다.
			PlaceReviewSummaryEntity savedSummary =
				placeReviewSummaryMapper.selectByPlaceId(placeId);
			if (savedSummary == null) {
				throw new IllegalStateException("Failed to find saved place review summary: " + placeId);
			}
			return toResponse(savedSummary);
		} catch (RuntimeException exception) {
			// AI 호출, 응답 파싱, 저장 과정에서 실패하면 FAILED 상태와 원인을 남긴다.
			// 예외는 관리자 API 호출자가 실패를 인지할 수 있도록 다시 던진다.
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

		// 전체 갱신은 한 장소가 실패해도 중단하지 않는다.
		// 실패한 placeId만 모아서 관리자에게 집계 결과로 반환한다.
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
		// 생성기 결과를 DB 저장용 Entity로 변환한다.
		// List<String> 필드는 MyBatis TypeHandler가 JSON 배열 컬럼으로 변환한다.
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
		// DB Entity에서 클라이언트 응답에 필요한 필드만 옮긴다.
		// errorMessage는 내부 진단용이므로 공개 응답에는 포함하지 않는다.
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
		// JSON 컬럼이 NULL이거나 TypeHandler 결과가 null인 경우에도
		// 응답에서는 빈 배열로 다루기 위해 빈 리스트로 정규화한다.
		if (values == null) {
			return Collections.emptyList();
		}
		return values;
	}

	private String getModelVersion() {
		// Fake 생성기를 사용하는 환경에서는 실제 모델명이 없으므로 구분 가능한 값으로 저장한다.
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
		// error_message 컬럼 길이가 VARCHAR(500)이므로 초과분은 잘라서 저장한다.
		if (errorMessage == null || errorMessage.isBlank()) {
			return "Unknown place review summary error";
		}
		if (errorMessage.length() <= 500) {
			return errorMessage;
		}
		return errorMessage.substring(0, 500);
	}
}
