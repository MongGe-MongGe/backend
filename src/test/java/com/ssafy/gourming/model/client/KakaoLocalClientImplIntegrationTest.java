package com.ssafy.gourming.model.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.ssafy.gourming.model.dto.KakaoPlaceDto;

@EnabledIfSystemProperty(
    named = "run.kakao.integration",
    matches = "true",
    disabledReason = "Kakao 실제 API 통합 테스트는 -Drun.kakao.integration=true 옵션이 있을 때만 실행됩니다."
)
class KakaoLocalClientImplIntegrationTest {

    @Test
    void searchPlaceByKeywordCallsRealKakaoLocalApi() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("application-local.properties")
        );

        String baseUrl = properties.getProperty(
            "kakao.local.base-url",
            "https://dapi.kakao.com"
        );
        String restApiKey = properties.getProperty("kakao.local.rest-api-key");

        assertThat(StringUtils.hasText(restApiKey))
            .as("kakao.local.rest-api-key 값이 application-local.properties에 필요합니다.")
            .isTrue();

        assertThat(restApiKey)
            .as("kakao.local.rest-api-key에는 예시 값이 아니라 실제 Kakao REST API 키를 넣어야 합니다.")
            .doesNotContain("REST_API_KEY");

        KakaoLocalClient client = new KakaoLocalClientImpl(
            RestClient.builder(),
            baseUrl,
            restApiKey
        );

        List<KakaoPlaceDto> places = client.searchPlaceByKeyword(
            "스타벅스",
            "127.027621",
            "37.497942"
        );

        System.out.println("Kakao Local API result count: " + places.size());

        places.stream()
            .limit(5)
            .forEach(place -> System.out.printf(
                "id=%s, name=%s, category=%s, roadAddress=%s, x=%s, y=%s%n",
                place.getId(),
                place.getName(),
                place.getCategoryName(),
                place.getRoadAddressName(),
                place.getX(),
                place.getY()
            ));

        assertThat(places).isNotEmpty();

        assertThat(places.getFirst().getId()).isNotBlank();
        assertThat(places.getFirst().getName()).isNotBlank();
        assertThat(places.getFirst().getX()).isNotBlank();
        assertThat(places.getFirst().getY()).isNotBlank();
    }
}