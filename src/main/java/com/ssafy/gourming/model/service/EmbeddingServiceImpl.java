package com.ssafy.gourming.model.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.config.EmbeddingProperties;
import com.ssafy.gourming.model.client.TextEmbedder;
import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingRefreshResponse;
import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingTargetRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.PlaceEmbeddingRow;
import com.ssafy.gourming.model.dto.EmbeddingDto.ReviewEmbeddingRow;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceEntity;
import com.ssafy.gourming.model.mapper.EmbeddingMapper;
import com.ssafy.gourming.model.mapper.PlaceMapper;
import com.ssafy.gourming.util.VectorMath;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

	private final EmbeddingMapper embeddingMapper;
	private final PlaceMapper placeMapper;
	private final TextEmbedder textEmbedder;
	private final EmbeddingProperties properties;

	private final AtomicBoolean running = new AtomicBoolean(false);

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public EmbeddingRefreshResponse refreshAll() {
		if (!running.compareAndSet(false, true)) {
			throw new IllegalStateException("Embedding refresh is already running");
		}
		LocalDateTime startedAt = LocalDateTime.now();
		String version = textEmbedder.version();
		EmbeddingRefreshResponse response = new EmbeddingRefreshResponse();
		response.setEmbedderVersion(version);
		response.setStartedAt(startedAt);
		try {
			refreshReviews(version, response);
			refreshPlaces(version, response);
			return response;
		} finally {
			response.setFinishedAt(LocalDateTime.now());
			running.set(false);
			log.info(
				"임베딩 배치 완료. version={}, reviews={}/{}/{}, places={}/{}/{}, elapsedMs={}",
				version,
				response.getReviewRequestedCount(),
				response.getReviewSuccessCount(),
				response.getReviewFailedCount(),
				response.getPlaceRequestedCount(),
				response.getPlaceSuccessCount(),
				response.getPlaceFailedCount(),
				Duration.between(startedAt, response.getFinishedAt()).toMillis()
			);
		}
	}

	private void refreshReviews(String version, EmbeddingRefreshResponse response) {
		// 대상을 한 번에 조회하고 자바에서 나눈다. 실패한 묶음이 계속 재조회되는 무한 루프를 막는다.
		List<EmbeddingTargetRow> targets = embeddingMapper.selectEmbeddingTargets(version);
		response.setReviewRequestedCount(targets.size());
		int batchSize = Math.max(1, properties.getBatchSize());

		for (int start = 0; start < targets.size(); start += batchSize) {
			List<EmbeddingTargetRow> chunk =
				targets.subList(start, Math.min(start + batchSize, targets.size()));
			try {
				// 임베딩 API 호출 중에는 DB 트랜잭션을 열지 않는다.
				List<float[]> vectors = textEmbedder.embed(
					chunk.stream().map(EmbeddingTargetRow::getContent).toList());
				List<ReviewEmbeddingRow> rows = new ArrayList<>(chunk.size());
				for (int i = 0; i < chunk.size(); i++) {
					ReviewEmbeddingRow row = new ReviewEmbeddingRow();
					row.setReviewId(chunk.get(i).getReviewId());
					row.setEmbedding(vectors.get(i));
					row.setEmbedderVersion(version);
					// 저장 시각이 아니라 본문을 읽은 시점의 리뷰 updated_at을 기록한다.
					row.setUpdatedAt(chunk.get(i).getReviewUpdatedAt());
					rows.add(row);
				}
				embeddingMapper.upsertReviewEmbeddings(rows);
				response.setReviewSuccessCount(response.getReviewSuccessCount() + chunk.size());
			} catch (RuntimeException exception) {
				// 리뷰 원문은 로그에 남기지 않는다.
				log.warn("리뷰 임베딩 묶음 실패. start={}, size={}, reason={}",
					start, chunk.size(), exception.getMessage());
				response.setReviewFailedCount(response.getReviewFailedCount() + chunk.size());
			}
		}
	}

	private void refreshPlaces(String version, EmbeddingRefreshResponse response) {
		List<String> placesWithReviews = embeddingMapper.selectPlaceIdsWithEmbeddedReviews(version);
		List<String> placesWithoutReviews =
			embeddingMapper.selectPlaceIdsNeedingCategoryEmbedding(version);
		response.setPlaceRequestedCount(placesWithReviews.size() + placesWithoutReviews.size());

		for (String placeId : placesWithReviews) {
			try {
				List<ReviewEmbeddingRow> rows =
					embeddingMapper.selectReviewEmbeddingsByPlaceId(placeId, version);
				if (rows.isEmpty()) {
					continue;
				}
				float[] mean = VectorMath.mean(
					rows.stream().map(ReviewEmbeddingRow::getEmbedding).toList());
				embeddingMapper.upsertPlaceEmbedding(
					placeRow(placeId, mean, PlaceEmbeddingRow.SOURCE_REVIEWS, rows.size(), version));
				response.setPlaceSuccessCount(response.getPlaceSuccessCount() + 1);
			} catch (RuntimeException exception) {
				log.warn("장소 벡터 계산 실패. placeId={}, reason={}", placeId, exception.getMessage());
				response.setPlaceFailedCount(response.getPlaceFailedCount() + 1);
			}
		}

		// 리뷰 없는 장소는 카테고리 문자열을 모아 한 번에 임베딩한다.
		int batchSize = Math.max(1, properties.getBatchSize());
		for (int start = 0; start < placesWithoutReviews.size(); start += batchSize) {
			List<String> chunk = placesWithoutReviews.subList(
				start, Math.min(start + batchSize, placesWithoutReviews.size()));
			try {
				List<String> texts = new ArrayList<>(chunk.size());
				for (String placeId : chunk) {
					PlaceEntity place = placeMapper.selectPlaceById(placeId);
					texts.add(place == null ? "" : place.getCategoryName() + " " + place.getName());
				}
				List<float[]> vectors = textEmbedder.embed(texts);
				for (int i = 0; i < chunk.size(); i++) {
					embeddingMapper.upsertPlaceEmbedding(placeRow(
						chunk.get(i), vectors.get(i), PlaceEmbeddingRow.SOURCE_CATEGORY, 0, version));
				}
				response.setPlaceSuccessCount(response.getPlaceSuccessCount() + chunk.size());
			} catch (RuntimeException exception) {
				log.warn("카테고리 임베딩 묶음 실패. start={}, size={}, reason={}",
					start, chunk.size(), exception.getMessage());
				response.setPlaceFailedCount(response.getPlaceFailedCount() + chunk.size());
			}
		}
	}

	private PlaceEmbeddingRow placeRow(
		String placeId,
		float[] embedding,
		String source,
		int reviewCount,
		String version
	) {
		PlaceEmbeddingRow row = new PlaceEmbeddingRow();
		row.setPlaceId(placeId);
		row.setEmbedding(embedding);
		row.setSource(source);
		row.setReviewCount(reviewCount);
		row.setEmbedderVersion(version);
		return row;
	}
}
