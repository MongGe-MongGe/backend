package com.ssafy.gourming.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.gourming.model.service.EmbeddingService;

@ExtendWith(MockitoExtension.class)
@DisplayName("임베딩 스케줄러 테스트")
class EmbeddingSchedulerTest {

	@Mock
	private EmbeddingService embeddingService;

	private EmbeddingScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new EmbeddingScheduler(embeddingService);
	}

	@Test
	@DisplayName("스케줄 실행 시 전체 갱신을 요청한다")
	void refreshEmbeddings() {
		when(embeddingService.isRunning()).thenReturn(false);

		scheduler.refreshEmbeddings();

		verify(embeddingService).refreshAll();
	}

	@Test
	@DisplayName("이미 실행 중이면 건너뛴다")
	void refreshEmbeddingsSkipsWhenRunning() {
		when(embeddingService.isRunning()).thenReturn(true);

		scheduler.refreshEmbeddings();

		verify(embeddingService, never()).refreshAll();
	}

	@Test
	@DisplayName("시작 직후 실행 설정이 꺼져 있으면 갱신하지 않는다")
	void refreshOnStartupSkipsWhenDisabled() {
		ReflectionTestUtils.setField(scheduler, "refreshOnStartup", false);

		scheduler.refreshOnStartup();

		verify(embeddingService, never()).refreshAll();
	}

	@Test
	@DisplayName("애플리케이션 시작 직후 실패해도 예외를 전파하지 않는다")
	void refreshOnStartupIgnoresFailure() {
		ReflectionTestUtils.setField(scheduler, "refreshOnStartup", true);
		when(embeddingService.isRunning()).thenReturn(false);
		doThrow(new RuntimeException("DB error")).when(embeddingService).refreshAll();

		assertThatCode(() -> scheduler.refreshOnStartup()).doesNotThrowAnyException();

		verify(embeddingService).refreshAll();
	}
}
