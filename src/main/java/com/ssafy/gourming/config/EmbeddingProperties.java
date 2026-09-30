package com.ssafy.gourming.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

	private Ai ai = new Ai();
	private int batchSize = 100;
	private String refreshCron = "0 5 0 * * *";

	@Getter
	@Setter
	public static class Ai {

		private boolean enabled;
		private String model;
		private String apiKey;
		private String baseUrl;
		private int timeoutSeconds = 20;
	}
}
