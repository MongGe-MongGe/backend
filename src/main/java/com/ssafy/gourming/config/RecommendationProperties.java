package com.ssafy.gourming.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

	private int candidateSize = 500;
	private int maxPerPlace = 2;
	private int seenExcludeDays = 14;
}
