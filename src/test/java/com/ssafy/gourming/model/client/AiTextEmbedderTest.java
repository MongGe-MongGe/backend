package com.ssafy.gourming.model.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.EmbeddingProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@DisplayName("AI 텍스트 임베더 테스트")
class AiTextEmbedderTest {

	private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
	private HttpServer server;
	private final AtomicReference<String> requestBody = new AtomicReference<>();
	private final AtomicInteger requestCount = new AtomicInteger();

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	@DisplayName("입력 배열을 한 번에 요청하고 응답 data를 index 순서로 매핑한다")
	void embedMapsResponseByIndex() throws IOException {
		// data를 일부러 역순으로 응답한다.
		startServer(200, objectMapper.writeValueAsString(Map.of(
			"data", List.of(
				Map.of("index", 1, "embedding", List.of(0.3, 0.4)),
				Map.of("index", 0, "embedding", List.of(0.1, 0.2))
			)
		)));
		AiTextEmbedder embedder = createEmbedder();

		List<float[]> result = embedder.embed(List.of("첫 번째", "두 번째"));

		JsonNode requestJson = objectMapper.readTree(requestBody.get());
		assertThat(requestJson.get("model").asText()).isEqualTo("text-embedding-3-small");
		assertThat(requestJson.get("input").size()).isEqualTo(2);
		assertThat(requestCount.get()).isEqualTo(1);
		assertThat(result.get(0)).containsExactly(0.1f, 0.2f);
		assertThat(result.get(1)).containsExactly(0.3f, 0.4f);
		assertThat(embedder.version()).isEqualTo("text-embedding-3-small");
	}

	@Test
	@DisplayName("응답 개수가 입력 개수와 다르면 예외를 던진다")
	void embedThrowsWhenCountMismatch() throws IOException {
		startServer(200, objectMapper.writeValueAsString(Map.of(
			"data", List.of(Map.of("index", 0, "embedding", List.of(0.1, 0.2)))
		)));
		AiTextEmbedder embedder = createEmbedder();

		assertThatThrownBy(() -> embedder.embed(List.of("첫 번째", "두 번째")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("count");
	}

	@Test
	@DisplayName("빈 입력은 API를 호출하지 않고 빈 결과를 반환한다")
	void embedSkipsApiForEmptyInput() throws IOException {
		startServer(200, "{}");
		AiTextEmbedder embedder = createEmbedder();

		List<float[]> result = embedder.embed(List.of());

		assertThat(result).isEmpty();
		assertThat(requestCount.get()).isZero();
	}

	@Test
	@DisplayName("공백 본문은 대체 문자열로 치환해 요청한다")
	void embedReplacesBlankText() throws IOException {
		startServer(200, objectMapper.writeValueAsString(Map.of(
			"data", List.of(Map.of("index", 0, "embedding", List.of(0.1)))
		)));
		AiTextEmbedder embedder = createEmbedder();

		embedder.embed(List.of("   "));

		JsonNode requestJson = objectMapper.readTree(requestBody.get());
		assertThat(requestJson.get("input").get(0).asText()).isEqualTo("(내용 없음)");
	}

	@Test
	@DisplayName("너무 긴 본문은 최대 길이로 잘라 요청한다")
	void embedTruncatesLongText() throws IOException {
		startServer(200, objectMapper.writeValueAsString(Map.of(
			"data", List.of(Map.of("index", 0, "embedding", List.of(0.1)))
		)));
		AiTextEmbedder embedder = createEmbedder();
		String longText = "가".repeat(AiTextEmbedder.MAX_TEXT_LENGTH + 500);

		embedder.embed(List.of(longText));

		JsonNode requestJson = objectMapper.readTree(requestBody.get());
		assertThat(requestJson.get("input").get(0).asText())
			.hasSize(AiTextEmbedder.MAX_TEXT_LENGTH);
	}

	@Test
	@DisplayName("API 키가 비어 있으면 호출 전에 예외를 던진다")
	void embedThrowsWhenApiKeyMissing() throws IOException {
		startServer(200, "{}");
		EmbeddingProperties properties = createProperties();
		properties.getAi().setApiKey("");
		AiTextEmbedder embedder = new AiTextEmbedder(RestClient.builder(), properties);

		assertThatThrownBy(() -> embedder.embed(List.of("본문")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("embedding.ai.api-key");
		assertThat(requestCount.get()).isZero();
	}

	private void startServer(int statusCode, String responseBody) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/embeddings", exchange -> {
			requestCount.incrementAndGet();
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			writeResponse(exchange, statusCode, responseBody);
		});
		server.start();
	}

	private void writeResponse(HttpExchange exchange, int statusCode, String responseBody)
		throws IOException {
		byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(statusCode, bytes.length);
		try (OutputStream stream = exchange.getResponseBody()) {
			stream.write(bytes);
		}
	}

	private EmbeddingProperties createProperties() {
		EmbeddingProperties properties = new EmbeddingProperties();
		properties.getAi().setEnabled(true);
		properties.getAi().setModel("text-embedding-3-small");
		properties.getAi().setApiKey("test-api-key");
		properties.getAi().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
		properties.getAi().setTimeoutSeconds(5);
		return properties;
	}

	private AiTextEmbedder createEmbedder() {
		return new AiTextEmbedder(RestClient.builder(), createProperties());
	}
}
