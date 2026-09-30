package com.ssafy.gourming.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.client.FakeTextEmbedder;
import com.ssafy.gourming.model.client.TextEmbedder;
import com.ssafy.gourming.model.dto.TasteTagDto.TasteTagRow;
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteEvidenceRow;
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteProfileResponse;
import com.ssafy.gourming.model.dto.UserTasteDto.UserTasteTagScore;
import com.ssafy.gourming.model.dto.UserTasteDto.UserVector;
import com.ssafy.gourming.model.mapper.TasteTagMapper;
import com.ssafy.gourming.model.mapper.UserTasteMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 성향 서비스 Mock 단위 테스트")
class UserTasteServiceMockTest {

	private static final String VERSION = "test-v1";
	private static final String USER_ID = "user-1";

	@Mock
	private UserTasteMapper userTasteMapper;

	@Mock
	private TasteTagMapper tasteTagMapper;

	@Mock
	private TextEmbedder textEmbedder;

	private UserTasteServiceImpl service;

	@BeforeEach
	void setUp() {
		when(textEmbedder.version()).thenReturn(VERSION);
		service = new UserTasteServiceImpl(userTasteMapper, tasteTagMapper, textEmbedder);
	}

	@Test
	@DisplayName("행동이 없으면 사용자 벡터는 null이고 빈 성향을 반환한다")
	void emptyProfileWithoutEvidence() {
		when(userTasteMapper.selectEvidences(USER_ID, VERSION)).thenReturn(List.of());

		assertThat(service.computeUserVector(USER_ID)).isNull();

		UserTasteProfileResponse response = service.getTasteProfile(USER_ID);
		assertThat(response.getEvidenceCount()).isZero();
		assertThat(response.getConfidence()).isEqualTo(0.0);
		assertThat(response.getTopTags()).isEmpty();
		verify(tasteTagMapper, never()).selectActiveTags();
	}

	@Test
	@DisplayName("가중치 합이 0 이하이면 사용자 벡터는 null이다")
	void nullVectorWhenWeightSumNotPositive() {
		when(userTasteMapper.selectEvidences(USER_ID, VERSION))
			.thenReturn(List.of(evidence(new float[] {1f, 0f}, -4), evidence(new float[] {0f, 1f}, 2)));

		assertThat(service.computeUserVector(USER_ID)).isNull();
	}

	@Test
	@DisplayName("사용자 벡터는 행동 벡터의 가중 평균이다")
	void computeUserVectorWeightedMean() {
		when(userTasteMapper.selectEvidences(USER_ID, VERSION))
			.thenReturn(List.of(evidence(new float[] {1f, 0f}, 3), evidence(new float[] {0f, 1f}, 1)));

		UserVector userVector = service.computeUserVector(USER_ID);

		assertThat(userVector.getEvidenceCount()).isEqualTo(2);
		assertThat(userVector.getVector()[0]).isCloseTo(0.75f, within(1e-6f));
		assertThat(userVector.getVector()[1]).isCloseTo(0.25f, within(1e-6f));
	}

	@Test
	@DisplayName("태그 유사도 내림차순 상위 5개와 confidence를 반환하고 벡터 없는·버전 다른 태그는 제외한다")
	void getTasteProfileRanksTags() {
		when(userTasteMapper.selectEvidences(USER_ID, VERSION))
			.thenReturn(List.of(evidence(new float[] {1f, 0f}, 5)));
		when(tasteTagMapper.selectActiveTags()).thenReturn(List.of(
			tag("a", new float[] {0f, 1f}, VERSION),
			tag("b", new float[] {1f, 0f}, VERSION),
			tag("c", new float[] {1f, 1f}, VERSION),
			tag("d", new float[] {1f, 0f}, "old-version"),
			tag("e", new float[0], null),
			tag("f", new float[] {0.9f, 0.1f}, VERSION),
			tag("g", new float[] {0.8f, 0.2f}, VERSION),
			tag("h", new float[] {0.7f, 0.3f}, VERSION),
			tag("i", new float[] {0.6f, 0.4f}, VERSION)
		));

		UserTasteProfileResponse response = service.getTasteProfile(USER_ID);

		assertThat(response.getEvidenceCount()).isEqualTo(1);
		assertThat(response.getConfidence()).isCloseTo(1.0 / 6.0, within(1e-9));
		assertThat(response.getTopTags()).hasSize(5);
		assertThat(response.getTopTags()).extracting(UserTasteTagScore::getCode)
			.containsExactly("b", "f", "g", "h", "i");
		assertThat(response.getTopTags().get(0).getScore()).isCloseTo(1.0, within(1e-6));
		assertThat(response.getTopTags().get(0).getLabel()).isEqualTo("b-label");
		assertThat(response.getTopTags().get(0).getCategory()).isEqualTo("MOOD");
	}

	@Test
	@DisplayName("Fake 임베더 기준 디저트 리뷰만 좋아요한 사용자의 1위 태그는 dessert다")
	void dessertLoverTopTagIsDessert() {
		FakeTextEmbedder fake = new FakeTextEmbedder();
		float[] dessertReview = fake.embed(List.of("케이크와 디저트가 맛있어요")).get(0);
		when(userTasteMapper.selectEvidences(USER_ID, VERSION))
			.thenReturn(List.of(evidence(dessertReview, 3)));
		when(tasteTagMapper.selectActiveTags()).thenReturn(List.of(
			tag("dessert", fake.embed(List.of("케이크, 빵, 디저트 메뉴가 맛있는 곳")).get(0), VERSION),
			tag("spicy", fake.embed(List.of("매운, 매콤한, 얼큰한 음식이 특징인 곳")).get(0), VERSION),
			tag("quiet", fake.embed(List.of("조용하고 차분해서 대화나 작업에 좋은 분위기")).get(0), VERSION)
		));

		UserTasteProfileResponse response = service.getTasteProfile(USER_ID);

		assertThat(response.getTopTags().get(0).getCode()).isEqualTo("dessert");
	}

	private UserTasteEvidenceRow evidence(float[] embedding, double weight) {
		UserTasteEvidenceRow row = new UserTasteEvidenceRow();
		row.setSource(UserTasteEvidenceRow.SOURCE_LIKE);
		row.setEmbedding(embedding);
		row.setWeight(weight);
		return row;
	}

	private TasteTagRow tag(String code, float[] embedding, String version) {
		TasteTagRow row = new TasteTagRow();
		row.setCode(code);
		row.setLabel(code + "-label");
		row.setCategory("MOOD");
		row.setEmbedding(embedding);
		row.setEmbedderVersion(version);
		row.setActive(true);
		return row;
	}
}
