package com.ssafy.gourming.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.EmbeddingDto.EmbeddingRefreshResponse;
import com.ssafy.gourming.model.service.EmbeddingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

	private final EmbeddingService embeddingService;

	// 관리자가 전체 리뷰·장소 벡터를 즉시 재계산한다. 스케줄 배치와 겹치면 409를 반환한다.
	@PutMapping("/refresh")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EmbeddingRefreshResponse> refresh() {
		if (embeddingService.isRunning()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		return ResponseEntity.ok(embeddingService.refreshAll());
	}
}
