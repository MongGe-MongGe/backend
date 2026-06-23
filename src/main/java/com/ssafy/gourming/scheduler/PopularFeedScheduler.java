package com.ssafy.gourming.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ssafy.gourming.model.service.PopularFeedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularFeedScheduler {

	private final PopularFeedService popularFeedService;

	@Value("${popular-feed.window-days:7}")
	private int popularWindowDays;

	@Scheduled(cron = "${popular-feed.refresh-cron:0 */10 * * * *}")
	public void refreshPopularFeed() {
		refreshPopularScores("scheduled");
	}

	@EventListener(ApplicationReadyEvent.class)
	public void refreshOnStartup() {
		try {
			refreshPopularScores("startup");
		} catch (Exception e) {
			log.warn("서버 시작 직후 인기피드 집계 실패. 다음 스케줄에서 재시도합니다.", e);
		}
	}

	private void refreshPopularScores(String trigger) {
		log.info("인기피드 집계 시작. trigger={}, windowDays={}", trigger, popularWindowDays);
		popularFeedService.refreshPopularScores(popularWindowDays);
		log.info("인기피드 집계 완료. trigger={}, windowDays={}", trigger, popularWindowDays);
	}
}
