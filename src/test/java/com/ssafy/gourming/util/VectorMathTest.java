package com.ssafy.gourming.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VectorMath 테스트")
class VectorMathTest {

	@Test
	@DisplayName("같은 벡터의 코사인은 1.0이고 직교 벡터는 0.0이며 영벡터는 0.0이다")
	void cosineBasics() {
		float[] a = {1f, 2f, 3f};
		float[] zero = {0f, 0f, 0f};

		assertThat(VectorMath.cosine(a, a)).isCloseTo(1.0, within(1e-6));
		assertThat(VectorMath.cosine(new float[] {1f, 0f}, new float[] {0f, 1f}))
			.isCloseTo(0.0, within(1e-6));
		assertThat(VectorMath.cosine(a, zero)).isEqualTo(0.0);
	}

	@Test
	@DisplayName("길이가 다른 벡터는 예외를 던진다")
	void cosineRejectsLengthMismatch() {
		assertThatThrownBy(() -> VectorMath.cosine(new float[] {1f}, new float[] {1f, 2f}))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("length");
	}

	@Test
	@DisplayName("가중 평균은 가중치 비율대로 계산된다")
	void weightedMeanUsesWeights() {
		float[] result = VectorMath.weightedMean(
			List.of(new float[] {1f, 0f}, new float[] {0f, 1f}),
			List.of(3.0, 1.0)
		);

		assertThat(result[0]).isCloseTo(0.75f, within(1e-6f));
		assertThat(result[1]).isCloseTo(0.25f, within(1e-6f));
	}

	@Test
	@DisplayName("음수 가중치를 포함해 합이 0 이하이면 예외를 던진다")
	void weightedMeanRejectsNonPositiveWeightSum() {
		assertThatThrownBy(() -> VectorMath.weightedMean(
			List.of(new float[] {1f}, new float[] {2f}),
			List.of(1.0, -1.0)
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("weight");
	}

	@Test
	@DisplayName("빈 목록이나 길이가 다른 벡터가 섞이면 예외를 던진다")
	void weightedMeanRejectsInvalidInput() {
		assertThatThrownBy(() -> VectorMath.mean(List.of()))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VectorMath.mean(List.of(new float[] {1f}, new float[] {1f, 2f})))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("length");
	}
}
