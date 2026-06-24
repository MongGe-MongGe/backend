package com.ssafy.gourming.model.client;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.PlaceSummaryAiProperties;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;

@Component
@ConditionalOnProperty(
	name = "place-summary.ai.enabled",
	havingValue = "true"
)
public class AiPlaceReviewSummarizer implements PlaceReviewSummarizer {

	private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
	private static final int MAX_LIST_SIZE = 5;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final PlaceSummaryAiProperties properties;

	public AiPlaceReviewSummarizer(
		RestClient.Builder builder,
		ObjectMapper objectMapper,
		PlaceSummaryAiProperties properties
	) {
		this.objectMapper = objectMapper;
		this.properties = properties;
		// baseUrl은 GMS 프록시 또는 OpenAI 호환 API 주소를 설정값으로 주입받는다.
		// timeout은 AI 응답 지연이 관리자 갱신 요청을 무한정 붙잡지 않도록 제한한다.
		this.restClient = builder
			.baseUrl(properties.getBaseUrl())
			.requestFactory(createRequestFactory(properties.getTimeoutSeconds()))
			.build();
	}

	@Override
	public PlaceReviewSummaryGenerateResult summarize(
		String placeId,
		List<ReviewSummarySourceRow> reviews
	) {
		// 호출부에서 null을 넘기더라도 리뷰가 없는 장소로 안전하게 처리한다.
		List<ReviewSummarySourceRow> safeReviews =
			reviews == null ? Collections.emptyList() : reviews;

		// 리뷰가 없으면 AI를 호출하지 않는다.
		// 이 경우는 실패가 아니라 정상적인 빈 요약(COMPLETED) 케이스다.
		if (safeReviews.isEmpty()) {
			return createEmptyResult();
		}

		// 비용과 prompt 길이를 제어하기 위해 최신 리뷰 일부만 AI에 전달한다.
		List<ReviewSummarySourceRow> limitedReviews = limitReviews(safeReviews);
		// AI 호출 -> 응답 content 추출 -> JSON 파싱 순서로 요약 결과를 만든다.
		ChatCompletionResponse response = requestSummary(placeId, limitedReviews);
		String content = extractContent(response);
		AiSummaryResponse summaryResponse = parseSummaryResponse(content);

		// AI 응답을 서비스 계층에서 저장 가능한 공통 생성 결과 DTO로 변환한다.
		PlaceReviewSummaryGenerateResult result = new PlaceReviewSummaryGenerateResult();
		result.setSummary(requireText(summaryResponse.getSummary(), "summary"));
		result.setPositivePoints(limitList(summaryResponse.getPositivePoints()));
		result.setNegativePoints(limitList(summaryResponse.getNegativePoints()));
		result.setRecommendedFor(limitList(summaryResponse.getRecommendedFor()));
		result.setKeywords(limitList(summaryResponse.getKeywords()));
		result.setReviewCount(safeReviews.size());
		result.setLastReviewUpdatedAt(findLastReviewUpdatedAt(safeReviews));
		return result;
	}

	private SimpleClientHttpRequestFactory createRequestFactory(int timeoutSeconds) {
		int safeTimeoutSeconds = timeoutSeconds <= 0 ? 20 : timeoutSeconds;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(safeTimeoutSeconds));
		requestFactory.setReadTimeout(Duration.ofSeconds(safeTimeoutSeconds));
		return requestFactory;
	}

	private PlaceReviewSummaryGenerateResult createEmptyResult() {
		// 리뷰 0개 장소는 AI 호출 없이 고정 문구와 빈 배열을 저장한다.
		PlaceReviewSummaryGenerateResult result = new PlaceReviewSummaryGenerateResult();
		result.setSummary("아직 작성된 리뷰가 없습니다.");
		result.setPositivePoints(List.of());
		result.setNegativePoints(List.of());
		result.setRecommendedFor(List.of());
		result.setKeywords(List.of());
		result.setReviewCount(0);
		result.setLastReviewUpdatedAt(null);
		return result;
	}

	private List<ReviewSummarySourceRow> limitReviews(List<ReviewSummarySourceRow> reviews) {
		int maxReviewCount = properties.getMaxReviewCount();
		// max-review-count가 0 이하이면 제한을 적용하지 않는다.
		if (maxReviewCount <= 0 || reviews.size() <= maxReviewCount) {
			return reviews;
		}
		return reviews.subList(0, maxReviewCount);
	}

	private ChatCompletionResponse requestSummary(
		String placeId,
		List<ReviewSummarySourceRow> reviews
	) {
		// 실제 AI 호출에 필요한 설정은 호출 직전에 검증해 설정 누락을 명확한 예외로 드러낸다.
		String model = requireSetting(properties.getModel(), "place-summary.ai.model");
		String apiKey = requireSetting(properties.getApiKey(), "place-summary.ai.api-key");

		// response_format=json_object를 넣어 모델이 JSON 외 텍스트를 섞어 보낼 가능성을 줄인다.
		ChatCompletionRequest request = new ChatCompletionRequest(
			model,
			List.of(
				new ChatMessage("developer", createDeveloperPrompt()),
				new ChatMessage("user", createUserPrompt(placeId, reviews))
			),
			new ResponseFormat("json_object")
		);

		return restClient.post()
			.uri(CHAT_COMPLETIONS_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.body(request)
			.retrieve()
			.body(ChatCompletionResponse.class);
	}

	private String createDeveloperPrompt() {
		// 모델의 역할과 출력 제약을 고정한다.
		// 사용자 리뷰는 신뢰할 수 없는 입력이므로 리뷰 밖 내용을 추측하지 않도록 제한한다.
		return """
			너는 음식점과 카페 리뷰를 한국어로 요약하는 assistant다.
			반드시 JSON 객체만 응답한다.
			마크다운 코드블록, 설명 문장, 주석은 절대 포함하지 않는다.
			리뷰에 없는 내용을 과장하거나 추측하지 않는다.
			summary는 1~2문장으로 작성한다.
			positivePoints, negativePoints, recommendedFor, keywords는 각각 최대 5개 문자열 배열이다.
			""";
	}

	private String createUserPrompt(
		String placeId,
		List<ReviewSummarySourceRow> reviews
	) {
		try {
			// 리뷰 원문은 JSON 문자열로 직렬화해 prompt에 포함한다.
			// 문자열 결합 과정에서 따옴표/개행이 깨지지 않도록 ObjectMapper를 사용한다.
			List<ReviewPromptItem> promptReviews = reviews.stream()
				.map(this::toPromptItem)
				.toList();

			return """
				아래 장소 리뷰들을 요약해줘.
				
				응답 JSON 형식:
				{
				  "summary": "최근 리뷰를 바탕으로 요약한 장소 설명",
				  "positivePoints": ["좋았던 점"],
				  "negativePoints": ["아쉬웠던 점"],
				  "recommendedFor": ["추천 대상"],
				  "keywords": ["키워드"]
				}
				
				placeId: %s
				reviews: %s
				""".formatted(placeId, objectMapper.writeValueAsString(promptReviews));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize review prompt", exception);
		}
	}

	private ReviewPromptItem toPromptItem(ReviewSummarySourceRow review) {
		// AI가 요약에 필요한 최소 정보만 받도록 prompt 전용 DTO로 축소한다.
		return new ReviewPromptItem(
			review.getReviewId(),
			review.getContent(),
			review.getRatingScore(),
			review.getVisitedAt(),
			review.getCreatedAt()
		);
	}

	private String extractContent(ChatCompletionResponse response) {
		// Chat Completions 응답에서 첫 번째 choice의 message.content만 사용한다.
		// 비어 있는 응답은 요약 실패로 처리해 서비스 계층에서 FAILED로 저장하게 한다.
		if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
			throw new IllegalStateException("AI summary response has no choices");
		}

		ChatChoice firstChoice = response.getChoices().get(0);
		if (
			firstChoice == null ||
			firstChoice.getMessage() == null ||
			firstChoice.getMessage().getContent() == null ||
			firstChoice.getMessage().getContent().isBlank()
		) {
			throw new IllegalStateException("AI summary response content is empty");
		}
		return firstChoice.getMessage().getContent();
	}

	private AiSummaryResponse parseSummaryResponse(String content) {
		// 모델이 혹시 JSON 앞뒤에 설명을 붙여도 파싱 가능하도록 JSON 객체 영역만 추출한다.
		String json = extractJsonObject(content);
		try {
			return objectMapper.readValue(json, AiSummaryResponse.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to parse AI summary response", exception);
		}
	}

	private String extractJsonObject(String content) {
		// 방어적 파싱: 첫 '{'부터 마지막 '}'까지를 JSON 객체로 간주한다.
		// response_format을 사용하더라도 프록시/모델 응답 편차에 대비한다.
		String trimmedContent = content.trim();
		int startIndex = trimmedContent.indexOf('{');
		int endIndex = trimmedContent.lastIndexOf('}');

		if (startIndex < 0 || endIndex < startIndex) {
			throw new IllegalStateException("AI summary response is not a JSON object");
		}
		return trimmedContent.substring(startIndex, endIndex + 1);
	}

	private String requireText(String value, String fieldName) {
		// summary는 사용자에게 노출되는 핵심 필드라 비어 있으면 실패로 본다.
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("AI summary response field is empty: " + fieldName);
		}
		return value.trim();
	}

	private String requireSetting(String value, String propertyName) {
		// API 키나 모델명이 누락된 상태로 외부 호출을 시도하지 않도록 막는다.
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("AI setting is empty: " + propertyName);
		}
		return value.trim();
	}

	private List<String> limitList(List<String> values) {
		// AI가 너무 많은 항목을 반환하거나 빈 문자열을 섞어 보내도 응답 계약을 일정하게 유지한다.
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream()
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(value -> !value.isBlank())
			.limit(MAX_LIST_SIZE)
			.toList();
	}

	private LocalDateTime findLastReviewUpdatedAt(List<ReviewSummarySourceRow> reviews) {
		// 요약이 어떤 리뷰 버전까지 반영했는지 판단할 수 있도록 최신 리뷰 수정 시각을 저장한다.
		return reviews.stream()
			.map(ReviewSummarySourceRow::getUpdatedAt)
			.filter(Objects::nonNull)
			.max(LocalDateTime::compareTo)
			.orElse(null);
	}

	private record ChatCompletionRequest(
		String model,
		List<ChatMessage> messages,
		@JsonProperty("response_format")
		ResponseFormat responseFormat
	) {
	}

	private record ResponseFormat(String type) {
	}

	private record ChatMessage(
		String role,
		String content
	) {
	}

	private record ReviewPromptItem(
		String reviewId,
		String content,
		Integer ratingScore,
		LocalDate visitedAt,
		LocalDateTime createdAt
	) {
	}

	public static class ChatCompletionResponse {

		private List<ChatChoice> choices;

		public List<ChatChoice> getChoices() {
			return choices;
		}

		public void setChoices(List<ChatChoice> choices) {
			this.choices = choices;
		}
	}

	public static class ChatChoice {

		private ChatMessageResponse message;

		public ChatMessageResponse getMessage() {
			return message;
		}

		public void setMessage(ChatMessageResponse message) {
			this.message = message;
		}
	}

	public static class ChatMessageResponse {

		private String content;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}

	public static class AiSummaryResponse {

		private String summary;
		private List<String> positivePoints;
		private List<String> negativePoints;
		private List<String> recommendedFor;
		private List<String> keywords;

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		public List<String> getPositivePoints() {
			return positivePoints;
		}

		public void setPositivePoints(List<String> positivePoints) {
			this.positivePoints = positivePoints;
		}

		public List<String> getNegativePoints() {
			return negativePoints;
		}

		public void setNegativePoints(List<String> negativePoints) {
			this.negativePoints = negativePoints;
		}

		public List<String> getRecommendedFor() {
			return recommendedFor;
		}

		public void setRecommendedFor(List<String> recommendedFor) {
			this.recommendedFor = recommendedFor;
		}

		public List<String> getKeywords() {
			return keywords;
		}

		public void setKeywords(List<String> keywords) {
			this.keywords = keywords;
		}
	}
}
