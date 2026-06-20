package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PostDto {

	@Getter
	@NoArgsConstructor
	public static class CreateRequest {
		@NotBlank(message = "제목은 필수 입력값입니다")
		private String title;

		@NotBlank(message = "내용은 필수 입력값입니다")
		private String content;

		@jakarta.validation.constraints.NotNull(message = "카테고리(Notice, Event)는 필수 입력값입니다")
		private PostCategory category;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CreateResponse {
		private String id;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PostResponse {
		private String id;
		private String title;
		private String content;
		private String category;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
		private String userId;
		private String authorNickname;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PostListResponse {
		private String id;
		private String title;
		private String category;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
		private String userId;
		private String authorNickname;
	}
	
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PostEntity {
		private String id;
		private String userId;
		private String title;
		private String content;
		private String category;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}
}
