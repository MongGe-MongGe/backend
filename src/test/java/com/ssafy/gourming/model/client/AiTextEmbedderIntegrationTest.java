package com.ssafy.gourming.model.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.ssafy.gourming.config.EmbeddingProperties;

@EnabledIfSystemProperty(
	named = "run.embedding.ai.integration",
	matches = "true",
	disabledReason = "임베딩 AI 통합 테스트는 -Drun.embedding.ai.integration=true 옵션이 있을 때만 실행됩니다."
)
@DisplayName("AI 텍스트 임베더 통합 테스트")
class AiTextEmbedderIntegrationTest {

	@Test
	@DisplayName("실제 GMS 임베딩 API를 호출해 1536차원 벡터를 받는다")
	void embedCallsGmsApi() throws IOException {
		Properties local = PropertiesLoaderUtils.loadProperties(
			new ClassPathResource("application-local.properties"));

		EmbeddingProperties properties = new EmbeddingProperties();
		properties.getAi().setEnabled(true);
		properties.getAi().setModel(local.getProperty("embedding.ai.model", "text-embedding-3-small"));
		properties.getAi().setApiKey(resolveApiKey(local));
		properties.getAi().setBaseUrl(local.getProperty(
			"embedding.ai.base-url",
			local.getProperty("place-summary.ai.base-url", "https://gms.ssafy.io/gmsapi/api.openai.com")));
		properties.getAi().setTimeoutSeconds(20);

		assertThat(properties.getAi().getApiKey())
			.as("GMS_KEY 환경변수 또는 place-summary.ai.api-key 값이 필요합니다.")
			.isNotBlank();

		AiTextEmbedder embedder = new AiTextEmbedder(RestClient.builder(), properties);

		List<float[]> result = embedder.embed(List.of("조용하고 케이크가 맛있는 카페"));

		assertThat(result).hasSize(1);
		assertThat(result.get(0)).hasSize(1536);
	}

	private String resolveApiKey(Properties properties) {
		String apiKey = properties.getProperty("embedding.ai.api-key");
		if (!StringUtils.hasText(apiKey) || apiKey.startsWith("${")) {
			apiKey = properties.getProperty("place-summary.ai.api-key");
		}
		if (!StringUtils.hasText(apiKey) || apiKey.startsWith("${")) {
			apiKey = System.getenv("GMS_KEY");
		}
		return apiKey;
	}
}
