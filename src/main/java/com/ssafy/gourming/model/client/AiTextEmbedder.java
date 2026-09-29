package com.ssafy.gourming.model.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ssafy.gourming.config.EmbeddingProperties;

@Component
@ConditionalOnProperty(
	name = "embedding.ai.enabled",
	havingValue = "true"
)
public class AiTextEmbedder implements TextEmbedder {

	private static final String EMBEDDINGS_PATH = "/v1/embeddings";
	private static final String BLANK_REPLACEMENT = "(내용 없음)";
	// 입력당 토큰 상한(8,192)을 넘는 리뷰 하나가 묶음 전체를 실패시키지 않도록 글자 수로 자른다.
	// 한글은 글자당 1~2토큰이라 3,000자면 여유가 있다.
	public static final int MAX_TEXT_LENGTH = 3000;

	private final RestClient restClient;
	private final EmbeddingProperties properties;

	public AiTextEmbedder(RestClient.Builder builder, EmbeddingProperties properties) {
		this.properties = properties;
		// baseUrl은 GMS 프록시 또는 OpenAI 호환 API 주소를 설정값으로 주입받는다.
		this.restClient = builder
			.baseUrl(properties.getAi().getBaseUrl())
			.requestFactory(createRequestFactory(properties.getAi().getTimeoutSeconds()))
			.build();
	}

	@Override
	public List<float[]> embed(List<String> texts) {
		if (texts == null || texts.isEmpty()) {
			return List.of();
		}
		String model = requireSetting(properties.getAi().getModel(), "embedding.ai.model");
		String apiKey = requireSetting(properties.getAi().getApiKey(), "embedding.ai.api-key");

		// 공백 문자열은 임베딩 API가 거부할 수 있어 고정 문자열로 치환하고, 너무 긴 본문은 자른다.
		List<String> input = texts.stream()
			.map(text -> text == null || text.isBlank() ? BLANK_REPLACEMENT : text)
			.map(text -> text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text)
			.toList();

		EmbeddingResponse response = restClient.post()
			.uri(EMBEDDINGS_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.body(new EmbeddingRequest(model, input))
			.retrieve()
			.body(EmbeddingResponse.class);

		if (response == null || response.getData() == null) {
			throw new IllegalStateException("Embedding response has no data");
		}
		if (response.getData().size() != input.size()) {
			throw new IllegalStateException(
				"Embedding response count mismatch: expected " + input.size()
					+ " but was " + response.getData().size());
		}

		// 응답 순서는 보장되지 않으므로 index로 정렬한다.
		List<EmbeddingData> sorted = new ArrayList<>(response.getData());
		sorted.sort(Comparator.comparingInt(EmbeddingData::getIndex));

		List<float[]> vectors = new ArrayList<>(sorted.size());
		for (EmbeddingData data : sorted) {
			if (data.getEmbedding() == null || data.getEmbedding().length == 0) {
				throw new IllegalStateException(
					"Embedding response has empty vector at index " + data.getIndex());
			}
			vectors.add(data.getEmbedding());
		}
		return vectors;
	}

	@Override
	public String version() {
		return properties.getAi().getModel();
	}

	private SimpleClientHttpRequestFactory createRequestFactory(int timeoutSeconds) {
		int safeTimeoutSeconds = timeoutSeconds <= 0 ? 20 : timeoutSeconds;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(safeTimeoutSeconds));
		requestFactory.setReadTimeout(Duration.ofSeconds(safeTimeoutSeconds));
		return requestFactory;
	}

	private String requireSetting(String value, String propertyName) {
		// API 키나 모델명이 누락된 상태로 외부 호출을 시도하지 않도록 막는다.
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("AI setting is empty: " + propertyName);
		}
		return value.trim();
	}

	private record EmbeddingRequest(String model, List<String> input) {
	}

	public static class EmbeddingResponse {

		private List<EmbeddingData> data;

		public List<EmbeddingData> getData() {
			return data;
		}

		public void setData(List<EmbeddingData> data) {
			this.data = data;
		}
	}

	public static class EmbeddingData {

		private int index;
		private float[] embedding;

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public float[] getEmbedding() {
			return embedding;
		}

		public void setEmbedding(float[] embedding) {
			this.embedding = embedding;
		}
	}
}
