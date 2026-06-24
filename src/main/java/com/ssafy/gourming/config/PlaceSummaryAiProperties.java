package com.ssafy.gourming.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "place-summary.ai")
public class PlaceSummaryAiProperties {

	private boolean enabled;
	private String provider;
	private String model;
	private String apiKey;
	private String baseUrl;
	private int timeoutSeconds;
	private int maxReviewCount;
}
