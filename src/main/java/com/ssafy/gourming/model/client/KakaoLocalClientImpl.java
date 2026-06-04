package com.ssafy.gourming.model.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ssafy.gourming.model.dto.KakaoPlaceDto;

@Component
public class KakaoLocalClientImpl implements KakaoLocalClient {
	
	private final RestClient restClient;
	private final String restApiKey;
	
	
	public KakaoLocalClientImpl(RestClient.Builder builder,
			@Value("${kakao.local.base-url}") String baseUrl, 
			@Value("${kakao.local.rest-api-key}")String restApiKey) {
		this.restClient = builder.baseUrl(baseUrl).build();
		this.restApiKey = restApiKey;
	}

	@Override
	public List<KakaoPlaceDto> searchPlaceByKeyword(String name, String x, String y) {
		
		KakaoPlaceDto.KeywordSearchResponse response = restClient.get()
				.uri(uriBuilder->uriBuilder
						.path("/v2/local/search/keyword.json")
						.queryParam("query", name)
						.queryParam("x", x)
						.queryParam("y", y)
						.queryParam("radius", 100)
						.queryParam("sort", "distance")
						.queryParam("size", 15)
						.build())
				.header(HttpHeaders.AUTHORIZATION, "KakaoAK "+restApiKey)
				.retrieve()
				.body(KakaoPlaceDto.KeywordSearchResponse.class);
		
		if (response == null || response.getDocuments() == null) {
		    return List.of();
		}

		return response.getDocuments();
	}

}
