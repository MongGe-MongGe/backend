package com.ssafy.gourming.model.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.client.TextEmbedder;
import com.ssafy.gourming.model.dto.TasteTagDto.TasteTagRow;
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteEvidenceRow;
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteProfileResponse;
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteTagScore;
import com.ssafy.gourming.model.dto.UserTasteDto.UserVector;
import com.ssafy.gourming.model.mapper.TasteTagMapper;
import com.ssafy.gourming.model.mapper.UserTasteMapper;
import com.ssafy.gourming.util.VectorMath;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserTasteServiceImpl implements UserTasteService {

	private static final int TOP_TAG_COUNT = 5;
	private static final double CONFIDENCE_SMOOTHING = 5.0;

	private final UserTasteMapper userTasteMapper;
	private final TasteTagMapper tasteTagMapper;
	private final TextEmbedder textEmbedder;

	@Override
	public UserVector computeUserVector(String userId) {
		// 사용자 벡터는 저장하지 않고 매번 원본 행동에서 다시 계산한다.
		// 좋아요 취소·저장 삭제·별점 수정이 다음 요청에 바로 반영된다.
		List<UserTasteEvidenceRow> evidences =
			userTasteMapper.selectEvidences(userId, textEmbedder.version());
		if (evidences.isEmpty()) {
			return null;
		}
		double weightSum = evidences.stream().mapToDouble(UserTasteEvidenceRow::getWeight).sum();
		if (weightSum <= 0.0) {
			// 별점 1~2점 리뷰만 있는 경우처럼 "좋아하는 방향"이 없으면 성향을 만들지 않는다.
			return null;
		}
		List<float[]> vectors = new ArrayList<>(evidences.size());
		List<Double> weights = new ArrayList<>(evidences.size());
		for (UserTasteEvidenceRow evidence : evidences) {
			vectors.add(evidence.getEmbedding());
			weights.add(evidence.getWeight());
		}
		return new UserVector(VectorMath.weightedMean(vectors, weights), evidences.size());
	}

	@Override
	public UserTasteProfileResponse getTasteProfile(String userId) {
		UserTasteProfileResponse response = new UserTasteProfileResponse();
		UserVector userVector = computeUserVector(userId);
		if (userVector == null) {
			response.setEvidenceCount(0);
			response.setConfidence(0.0);
			response.setTopTags(List.of());
			return response;
		}

		String version = textEmbedder.version();
		List<UserTasteTagScore> scores = new ArrayList<>();
		for (TasteTagRow tag : tasteTagMapper.selectActiveTags()) {
			// 배치 전이거나 버전이 다른 태그 벡터는 비교하지 않는다. 길이가 달라 코사인이 예외를 던진다.
			if (tag.getEmbedding() == null || tag.getEmbedding().length == 0
				|| !version.equals(tag.getEmbedderVersion())) {
				continue;
			}
			UserTasteTagScore score = new UserTasteTagScore();
			score.setCode(tag.getCode());
			score.setLabel(tag.getLabel());
			score.setCategory(tag.getCategory());
			score.setScore(VectorMath.cosine(userVector.getVector(), tag.getEmbedding()));
			scores.add(score);
		}
		// 점수 임계값은 두지 않는다. 임베딩 모델마다 유사도 분포가 달라 고정값은 모델 교체 시 깨진다.
		scores.sort(Comparator.comparingDouble(UserTasteTagScore::getScore).reversed());

		int evidenceCount = userVector.getEvidenceCount();
		response.setEvidenceCount(evidenceCount);
		response.setConfidence(evidenceCount / (evidenceCount + CONFIDENCE_SMOOTHING));
		response.setTopTags(new ArrayList<>(scores.subList(0, Math.min(TOP_TAG_COUNT, scores.size()))));
		return response;
	}
}
