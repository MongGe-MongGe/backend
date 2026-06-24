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
		PlaceReviewSummaryEntity existingSummary =
			placeReviewSummaryMapper.selectByPlaceId(placeId);

		if (existingSummary == null) {
			return null;
		}
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
		placeReviewSummaryMapper.markProcessing(placeId, modelVersion);

		try {
			List<ReviewSummarySourceRow> reviews =
				placeReviewSummaryMapper.selectReviewSourcesByPlaceId(placeId);

			PlaceReviewSummaryGenerateResult generatedSummary =
				placeReviewSummarizer.summarize(placeId, reviews);

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
		if (errorMessage == null || errorMessage.isBlank()) {
			return "Unknown place review summary error";
		}
		if (errorMessage.length() <= 500) {
			return errorMessage;
		}
		return errorMessage.substring(0, 500);
	}
}
