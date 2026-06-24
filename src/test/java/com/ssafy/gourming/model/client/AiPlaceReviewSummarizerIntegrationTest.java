package com.ssafy.gourming.model.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.PlaceSummaryAiProperties;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryGenerateResult;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;

@EnabledIfSystemProperty(
	named = "run.place-summary.ai.integration",
	matches = "true",
	disabledReason = "장소 리뷰 요약 AI 통합 테스트는 -Drun.place-summary.ai.integration=true 옵션이 있을 때만 실행됩니다."
)
@DisplayName("장소 리뷰 요약 AI 클라이언트 통합 테스트")
class AiPlaceReviewSummarizerIntegrationTest {

	@Test
	@DisplayName("실제 GMS OpenAI 호환 API를 호출해 장소 리뷰 요약을 생성한다")
	void summarizeCallsGmsApi() throws IOException {
		PlaceSummaryAiProperties properties = loadProperties();
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		AiPlaceReviewSummarizer summarizer = new AiPlaceReviewSummarizer(
			RestClient.builder(),
			objectMapper,
			properties
		);

		PlaceReviewSummaryGenerateResult result = summarizer.summarize(
			"test-place-ai-summary",
			List.of(createReviewSource())
		);

		System.out.printf(
			"AI summary result: summary=%s, positivePoints=%s, negativePoints=%s, recommendedFor=%s, keywords=%s%n",
			result.getSummary(),
			result.getPositivePoints(),
			result.getNegativePoints(),
			result.getRecommendedFor(),
			result.getKeywords()
		);

		assertThat(result.getSummary()).isNotBlank();
		assertThat(result.getPositivePoints()).isNotNull();
		assertThat(result.getNegativePoints()).isNotNull();
		assertThat(result.getRecommendedFor()).isNotNull();
		assertThat(result.getKeywords()).isNotNull();
		assertThat(result.getReviewCount()).isEqualTo(1);
		assertThat(result.getLastReviewUpdatedAt())
			.isEqualTo(LocalDateTime.of(2026, 6, 24, 12, 0));
	}

	private PlaceSummaryAiProperties loadProperties() throws IOException {
		Properties localProperties = PropertiesLoaderUtils.loadProperties(
			new ClassPathResource("application-local.properties")
		);

		PlaceSummaryAiProperties properties = new PlaceSummaryAiProperties();
		properties.setEnabled(true);
		properties.setProvider(localProperties.getProperty(
			"place-summary.ai.provider",
			"openai"
		));
		properties.setModel(localProperties.getProperty(
			"place-summary.ai.model",
			"gpt-5-mini"
		));
		properties.setApiKey(resolveApiKey(localProperties));
		properties.setBaseUrl(localProperties.getProperty(
			"place-summary.ai.base-url",
			"https://gms.ssafy.io/gmsapi/api.openai.com"
		));
		properties.setTimeoutSeconds(Integer.parseInt(localProperties.getProperty(
			"place-summary.ai.timeout-seconds",
			"20"
		)));
		properties.setMaxReviewCount(Integer.parseInt(localProperties.getProperty(
			"place-summary.ai.max-review-count",
			"50"
		)));

		assertThat(properties.getProvider()).isEqualTo("openai");
		assertThat(properties.getModel()).isEqualTo("gpt-5-mini");
		assertThat(properties.getApiKey())
			.as("GMS_KEY 환경변수 또는 place-summary.ai.api-key 값이 필요합니다.")
			.isNotBlank()
			.doesNotContain("${");
		assertThat(properties.getBaseUrl())
			.isEqualTo("https://gms.ssafy.io/gmsapi/api.openai.com");

		return properties;
	}

	private String resolveApiKey(Properties properties) {
		String apiKey = properties.getProperty("place-summary.ai.api-key");
		if ("${GMS_KEY}".equals(apiKey) || !StringUtils.hasText(apiKey)) {
			return System.getenv("GMS_KEY");
		}
		return apiKey;
	}

	private ReviewSummarySourceRow createReviewSource() {
		ReviewSummarySourceRow review = new ReviewSummarySourceRow();
		review.setReviewId("test-review-ai-summary");
		review.setContent("파스타가 맛있고 분위기가 좋아요. 직원도 친절했지만, 저녁 시간에는 웨이팅이 조금 있었어요.");
		review.setRatingScore(4);
		review.setVisitedAt(LocalDate.of(2026, 6, 24));
		review.setCreatedAt(LocalDateTime.of(2026, 6, 24, 11, 0));
		review.setUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 0));
		return review;
	}
}
