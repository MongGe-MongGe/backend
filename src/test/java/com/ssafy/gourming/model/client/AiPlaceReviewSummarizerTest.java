package com.ssafy.gourming.model.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.PlaceSummaryAiProperties;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@DisplayName("AI 장소 리뷰 요약 생성기 테스트")
class AiPlaceReviewSummarizerTest {

	private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	@DisplayName("GMS OpenAI 호환 API에 response_format을 포함해 요청하고 요약 결과를 파싱한다")
	void summarizeCallsChatCompletionsWithResponseFormat() throws IOException {
		AtomicReference<String> requestBody = new AtomicReference<>();
		startServer(
			HttpStatus.OK.value(),
			createChatCompletionResponse(createAiSummaryJson()),
			requestBody,
			new AtomicInteger()
		);
		AiPlaceReviewSummarizer summarizer = createSummarizer();

		PlaceReviewSummaryGenerateResult result = summarizer.summarize(
			"test-place",
			List.of(createReviewSource("review-1", 4, LocalDateTime.of(2026, 6, 24, 12, 0)))
		);

		JsonNode requestJson = objectMapper.readTree(requestBody.get());

		assertThat(requestJson.get("model").asText()).isEqualTo("gpt-5-mini");
		assertThat(requestJson.get("response_format").get("type").asText())
			.isEqualTo("json_object");
		assertThat(requestJson.get("messages").size()).isEqualTo(2);
		assertThat(result.getSummary()).isEqualTo("음식 맛과 분위기에 대한 만족도가 높은 장소입니다.");
		assertThat(result.getPositivePoints()).containsExactly("맛이 좋아요");
		assertThat(result.getNegativePoints()).containsExactly("대기 시간이 길 수 있어요");
		assertThat(result.getRecommendedFor()).containsExactly("데이트");
		assertThat(result.getKeywords()).containsExactly("파스타", "분위기");
		assertThat(result.getReviewCount()).isEqualTo(1);
		assertThat(result.getLastReviewUpdatedAt())
			.isEqualTo(LocalDateTime.of(2026, 6, 24, 12, 0));
	}

	@Test
	@DisplayName("리뷰가 없으면 API를 호출하지 않고 빈 요약을 반환한다")
	void summarizeWithoutReviewsDoesNotCallApi() throws IOException {
		AtomicInteger requestCount = new AtomicInteger();
		startServer(
			HttpStatus.OK.value(),
			createChatCompletionResponse(createAiSummaryJson()),
			new AtomicReference<>(),
			requestCount
		);
		AiPlaceReviewSummarizer summarizer = createSummarizer();

		PlaceReviewSummaryGenerateResult result =
			summarizer.summarize("test-place", List.of());

		assertThat(requestCount.get()).isZero();
		assertThat(result.getSummary()).isEqualTo("아직 작성된 리뷰가 없습니다.");
		assertThat(result.getPositivePoints()).isEmpty();
		assertThat(result.getNegativePoints()).isEmpty();
		assertThat(result.getRecommendedFor()).isEmpty();
		assertThat(result.getKeywords()).isEmpty();
		assertThat(result.getReviewCount()).isZero();
		assertThat(result.getLastReviewUpdatedAt()).isNull();
	}

	@Test
	@DisplayName("AI 응답 choices가 비어 있으면 예외를 던진다")
	void summarizeThrowsWhenChoicesEmpty() throws IOException {
		startServer(
			HttpStatus.OK.value(),
			"""
			{"choices":[]}
			""",
			new AtomicReference<>(),
			new AtomicInteger()
		);
		AiPlaceReviewSummarizer summarizer = createSummarizer();

		assertThatThrownBy(() -> summarizer.summarize(
			"test-place",
			List.of(createReviewSource("review-1", 4, LocalDateTime.of(2026, 6, 24, 12, 0)))
		))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("no choices");
	}

	@Test
	@DisplayName("AI 응답 content가 비어 있으면 예외를 던진다")
	void summarizeThrowsWhenContentBlank() throws IOException {
		startServer(
			HttpStatus.OK.value(),
			"""
			{"choices":[{"message":{"content":"   "}}]}
			""",
			new AtomicReference<>(),
			new AtomicInteger()
		);
		AiPlaceReviewSummarizer summarizer = createSummarizer();

		assertThatThrownBy(() -> summarizer.summarize(
			"test-place",
			List.of(createReviewSource("review-1", 4, LocalDateTime.of(2026, 6, 24, 12, 0)))
		))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("content is empty");
	}

	@Test
	@DisplayName("AI 응답 content가 JSON 객체가 아니면 예외를 던진다")
	void summarizeThrowsWhenContentIsNotJsonObject() throws IOException {
		startServer(
			HttpStatus.OK.value(),
			createChatCompletionResponse("요약 결과입니다."),
			new AtomicReference<>(),
			new AtomicInteger()
		);
		AiPlaceReviewSummarizer summarizer = createSummarizer();

		assertThatThrownBy(() -> summarizer.summarize(
			"test-place",
			List.of(createReviewSource("review-1", 4, LocalDateTime.of(2026, 6, 24, 12, 0)))
		))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("not a JSON object");
	}

	@Test
	@DisplayName("AI 응답 summary가 비어 있으면 예외를 던진다")
	void summarizeThrowsWhenSummaryMissing() throws IOException {
		startServer(
			HttpStatus.OK.value(),
			createChatCompletionResponse(
				"""
				{"positivePoints":[],"negativePoints":[],"recommendedFor":[],"keywords":[]}
				"""
			),
			new AtomicReference<>(),
			new AtomicInteger()
		);
		AiPlaceReviewSummarizer summarizer = createSummarizer();

		assertThatThrownBy(() -> summarizer.summarize(
			"test-place",
			List.of(createReviewSource("review-1", 4, LocalDateTime.of(2026, 6, 24, 12, 0)))
		))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("summary");
	}

	@Test
	@DisplayName("AI 응답 배열 필드는 최대 5개로 제한한다")
	void summarizeLimitsListFieldsToFiveItems() throws IOException {
		startServer(
			HttpStatus.OK.value(),
			createChatCompletionResponse(
				"""
				{
				  "summary": "요약입니다.",
				  "positivePoints": ["1", "2", "3", "4", "5", "6"],
				  "negativePoints": [],
				  "recommendedFor": ["1", "2", "3", "4", "5", "6"],
				  "keywords": ["1", "2", "3", "4", "5", "6"]
				}
				"""
			),
			new AtomicReference<>(),
			new AtomicInteger()
		);
		AiPlaceReviewSummarizer summarizer = createSummarizer();

		PlaceReviewSummaryGenerateResult result = summarizer.summarize(
			"test-place",
			List.of(createReviewSource("review-1", 4, LocalDateTime.of(2026, 6, 24, 12, 0)))
		);

		assertThat(result.getPositivePoints()).containsExactly("1", "2", "3", "4", "5");
		assertThat(result.getRecommendedFor()).containsExactly("1", "2", "3", "4", "5");
		assertThat(result.getKeywords()).containsExactly("1", "2", "3", "4", "5");
	}

	private void startServer(
		int statusCode,
		String responseBody,
		AtomicReference<String> requestBody,
		AtomicInteger requestCount
	) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/chat/completions", exchange -> {
			requestCount.incrementAndGet();
			requestBody.set(readRequestBody(exchange));
			writeResponse(exchange, statusCode, responseBody);
		});
		server.start();
	}

	private String readRequestBody(HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private void writeResponse(
		HttpExchange exchange,
		int statusCode,
		String responseBody
	) throws IOException {
		byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(statusCode, responseBytes.length);
		try (OutputStream responseStream = exchange.getResponseBody()) {
			responseStream.write(responseBytes);
		}
	}

	private AiPlaceReviewSummarizer createSummarizer() {
		PlaceSummaryAiProperties properties = new PlaceSummaryAiProperties();
		properties.setEnabled(true);
		properties.setProvider("openai");
		properties.setModel("gpt-5-mini");
		properties.setApiKey("test-api-key");
		properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
		properties.setTimeoutSeconds(5);
		properties.setMaxReviewCount(50);

		return new AiPlaceReviewSummarizer(
			RestClient.builder(),
			objectMapper,
			properties
		);
	}

	private String createChatCompletionResponse(String content) throws IOException {
		return objectMapper.writeValueAsString(Map.of(
			"choices",
			List.of(Map.of(
				"message",
				Map.of("content", content)
			))
		));
	}

	private String createAiSummaryJson() {
		return """
			{
			  "summary": "음식 맛과 분위기에 대한 만족도가 높은 장소입니다.",
			  "positivePoints": ["맛이 좋아요"],
			  "negativePoints": ["대기 시간이 길 수 있어요"],
			  "recommendedFor": ["데이트"],
			  "keywords": ["파스타", "분위기"]
			}
			""";
	}

	private ReviewSummarySourceRow createReviewSource(
		String reviewId,
		Integer ratingScore,
		LocalDateTime updatedAt
	) {
		ReviewSummarySourceRow review = new ReviewSummarySourceRow();
		review.setReviewId(reviewId);
		review.setContent("파스타가 맛있고 분위기가 좋아요.");
		review.setRatingScore(ratingScore);
		review.setVisitedAt(LocalDate.of(2026, 6, 24));
		review.setCreatedAt(updatedAt.minusHours(1));
		review.setUpdatedAt(updatedAt);
		return review;
	}
}
