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
		List<ReviewSummarySourceRow> safeReviews =
			reviews == null ? Collections.emptyList() : reviews;

		if (safeReviews.isEmpty()) {
			return createEmptyResult();
		}

		List<ReviewSummarySourceRow> limitedReviews = limitReviews(safeReviews);
		ChatCompletionResponse response = requestSummary(placeId, limitedReviews);
		String content = extractContent(response);
		AiSummaryResponse summaryResponse = parseSummaryResponse(content);

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
		if (maxReviewCount <= 0 || reviews.size() <= maxReviewCount) {
			return reviews;
		}
		return reviews.subList(0, maxReviewCount);
	}

	private ChatCompletionResponse requestSummary(
		String placeId,
		List<ReviewSummarySourceRow> reviews
	) {
		String model = requireSetting(properties.getModel(), "place-summary.ai.model");
		String apiKey = requireSetting(properties.getApiKey(), "place-summary.ai.api-key");

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
		return new ReviewPromptItem(
			review.getReviewId(),
			review.getContent(),
			review.getRatingScore(),
			review.getVisitedAt(),
			review.getCreatedAt()
		);
	}

	private String extractContent(ChatCompletionResponse response) {
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
		String json = extractJsonObject(content);
		try {
			return objectMapper.readValue(json, AiSummaryResponse.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to parse AI summary response", exception);
		}
	}

	private String extractJsonObject(String content) {
		String trimmedContent = content.trim();
		int startIndex = trimmedContent.indexOf('{');
		int endIndex = trimmedContent.lastIndexOf('}');

		if (startIndex < 0 || endIndex < startIndex) {
			throw new IllegalStateException("AI summary response is not a JSON object");
		}
		return trimmedContent.substring(startIndex, endIndex + 1);
	}

	private String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("AI summary response field is empty: " + fieldName);
		}
		return value.trim();
	}

	private String requireSetting(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("AI setting is empty: " + propertyName);
		}
		return value.trim();
	}

	private List<String> limitList(List<String> values) {
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
