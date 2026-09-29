package com.ssafy.gourming.util;

import java.util.Collections;
import java.util.List;

// 임베딩 벡터 계산 유틸. 추천·성향 계산은 이 두 연산만 사용한다.
public final class VectorMath {

	private VectorMath() {
	}

	public static float[] mean(List<float[]> vectors) {
		return weightedMean(vectors, Collections.nCopies(vectors.size(), 1.0));
	}

	public static float[] weightedMean(List<float[]> vectors, List<Double> weights) {
		if (vectors == null || vectors.isEmpty()) {
			throw new IllegalArgumentException("Vectors must not be empty");
		}
		if (weights == null || weights.size() != vectors.size()) {
			throw new IllegalArgumentException("Weights size must match vectors size");
		}
		int length = vectors.get(0).length;
		double weightSum = 0.0;
		double[] sum = new double[length];

		for (int i = 0; i < vectors.size(); i++) {
			float[] vector = vectors.get(i);
			if (vector.length != length) {
				throw new IllegalArgumentException(
					"Vector length mismatch: " + vector.length + " vs " + length);
			}
			double weight = weights.get(i);
			weightSum += weight;
			for (int d = 0; d < length; d++) {
				sum[d] += vector[d] * weight;
			}
		}
		if (weightSum <= 0.0) {
			throw new IllegalArgumentException("Sum of weights must be positive: " + weightSum);
		}

		float[] result = new float[length];
		for (int d = 0; d < length; d++) {
			result[d] = (float) (sum[d] / weightSum);
		}
		return result;
	}

	public static double cosine(float[] a, float[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException(
				"Vector length mismatch: " + a.length + " vs " + b.length);
		}
		double dot = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
			normA += a[i] * a[i];
			normB += b[i] * b[i];
		}
		if (normA == 0.0 || normB == 0.0) {
			return 0.0;
		}
		return dot / (Math.sqrt(normA) * Math.sqrt(normB));
	}
}
