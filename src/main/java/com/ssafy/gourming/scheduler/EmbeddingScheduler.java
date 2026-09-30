package com.ssafy.gourming.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ssafy.gourming.model.service.EmbeddingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingScheduler {

	private final EmbeddingService embeddingService;

	@Scheduled(cron = "${embedding.refresh-cron:0 5 0 * * *}", zone = "Asia/Seoul")
	public void refreshEmbeddings() {
		refresh("scheduled");
	}

	@EventListener(ApplicationReadyEvent.class)
	public void refreshOnStartup() {
		try {
			refresh("startup");
		} catch (Exception e) {
			log.warn("서버 시작 직후 임베딩 갱신 실패. 다음 스케줄에서 재시도합니다.", e);
		}
	}

	private void refresh(String trigger) {
		if (embeddingService.isRunning()) {
			log.info("임베딩 갱신이 이미 실행 중이라 건너뜁니다. trigger={}", trigger);
			return;
		}
		log.info("임베딩 갱신 시작. trigger={}", trigger);
		embeddingService.refreshAll();
		log.info("임베딩 갱신 완료. trigger={}", trigger);
	}
}
