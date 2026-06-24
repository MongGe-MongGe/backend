package com.ssafy.gourming.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.gourming.model.service.PopularFeedService;

@ExtendWith(MockitoExtension.class)
@DisplayName("인기피드 스케줄러 테스트")
class PopularFeedSchedulerTest {

	private static final int WINDOW_DAYS = 7;

	@Mock
	private PopularFeedService popularFeedService;

	private PopularFeedScheduler popularFeedScheduler;

	@BeforeEach
	void setUp() {
		popularFeedScheduler = new PopularFeedScheduler(popularFeedService);
		ReflectionTestUtils.setField(popularFeedScheduler, "popularWindowDays", WINDOW_DAYS);
	}

	@Test
	@DisplayName("스케줄 실행 시 설정된 windowDays로 인기피드 집계를 요청한다")
	void refreshPopularFeed() {
		popularFeedScheduler.refreshPopularFeed();

		verify(popularFeedService).refreshPopularScores(WINDOW_DAYS);
	}

	@Test
	@DisplayName("애플리케이션 시작 직후 설정된 windowDays로 인기피드 집계를 요청한다")
	void refreshOnStartup() {
		popularFeedScheduler.refreshOnStartup();

		verify(popularFeedService).refreshPopularScores(WINDOW_DAYS);
	}

	@Test
	@DisplayName("애플리케이션 시작 직후 집계가 실패해도 예외를 전파하지 않는다")
	void refreshOnStartupIgnoresFailure() {
		doThrow(new RuntimeException("DB error"))
			.when(popularFeedService)
			.refreshPopularScores(WINDOW_DAYS);

		assertThatCode(() -> popularFeedScheduler.refreshOnStartup())
			.doesNotThrowAnyException();

		verify(popularFeedService).refreshPopularScores(WINDOW_DAYS);
	}
}
