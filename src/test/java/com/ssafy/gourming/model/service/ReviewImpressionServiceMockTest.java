package com.ssafy.gourming.model.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.mapper.ReviewImpressionMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("리뷰 노출 기록 서비스 Mock 단위 테스트")
class ReviewImpressionServiceMockTest {

	@Mock
	private ReviewImpressionMapper reviewImpressionMapper;

	@InjectMocks
	private ReviewImpressionServiceImpl service;

	@Test
	@DisplayName("중복·공백 ID를 제거하고 순서를 유지해 Mapper에 전달한다")
	void recordDeduplicatesAndKeepsOrder() {
		service.record("user-1", Arrays.asList("r2", "r1", "r2", " ", null, "r3", "r1"));

		verify(reviewImpressionMapper).upsertImpressions("user-1", List.of("r2", "r1", "r3"));
	}

	@Test
	@DisplayName("남은 ID가 없으면 Mapper를 호출하지 않는다")
	void recordSkipsWhenNothingLeft() {
		service.record("user-1", Arrays.asList(" ", null));

		verify(reviewImpressionMapper, never()).upsertImpressions(any(), anyList());
	}
}
