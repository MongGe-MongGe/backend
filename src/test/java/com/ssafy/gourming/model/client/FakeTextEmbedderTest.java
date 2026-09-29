package com.ssafy.gourming.model.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fake 텍스트 임베더 테스트")
class FakeTextEmbedderTest {

	private final FakeTextEmbedder embedder = new FakeTextEmbedder();

	@Test
	@DisplayName("키워드가 포함된 본문은 해당 차원이 1 이상이다")
	void embedSetsDimensionForKeyword() {
		List<float[]> result = embedder.embed(List.of("조용하고 케이크가 맛있어요"));

		float[] vector = result.get(0);
		assertThat(vector).hasSize(FakeTextEmbedder.DIMENSION);
		assertThat(vector[FakeTextEmbedder.indexOf("dessert")]).isGreaterThanOrEqualTo(1f);
		assertThat(vector[FakeTextEmbedder.indexOf("quiet")]).isGreaterThanOrEqualTo(1f);
		assertThat(vector[FakeTextEmbedder.indexOf("spicy")]).isEqualTo(0f);
	}

	@Test
	@DisplayName("키워드가 없는 본문은 영벡터다")
	void embedReturnsZeroVectorWithoutKeyword() {
		List<float[]> result = embedder.embed(List.of("그냥 그랬어요"));

		assertThat(result.get(0)).containsOnly(0f);
	}

	@Test
	@DisplayName("입력 순서대로 결과를 반환하고 빈 입력은 빈 결과다")
	void embedKeepsOrder() {
		List<float[]> result = embedder.embed(List.of("매운 떡볶이", "케이크"));

		assertThat(result).hasSize(2);
		assertThat(result.get(0)[FakeTextEmbedder.indexOf("spicy")]).isEqualTo(1f);
		assertThat(result.get(1)[FakeTextEmbedder.indexOf("dessert")]).isEqualTo(1f);
		assertThat(embedder.embed(List.of())).isEmpty();
		assertThat(embedder.version()).isEqualTo("fake-v1");
	}
}
