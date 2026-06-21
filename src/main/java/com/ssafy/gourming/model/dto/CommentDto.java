package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CommentDto {

	// comments 테이블과 매핑되는 내부 엔티티
	@Getter
	@Setter
	@NoArgsConstructor
	public static class CommentEntity {

		private String id;
		private String userId;
		private String reviewId;
		private String content;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}

	// 댓글 작성 요청
	@Getter
	@Setter
	@NoArgsConstructor
	public static class CommentCreateRequest {

		@NotBlank(message = "Comment content is required.")
		private String content;
	}

	// 댓글 수정 요청
	@Getter
	@Setter
	@NoArgsConstructor
	public static class CommentUpdateRequest {

		@NotBlank(message = "Comment content is required.")
		private String content;
	}

	// 댓글 작성, 수정 및 목록 조회 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class CommentResponse {

		private String id;
		private String reviewId;
		private String content;
		private AuthorSummary author;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}

	// 페이지네이션이 적용된 댓글 목록 응답
	@Getter
	@Setter
	@NoArgsConstructor
	public static class CommentPageResponse {

		private List<CommentResponse> content;
		private int page;
		private int size;
		private long totalElements;
		private int totalPages;
		private boolean first;
		private boolean last;
	}

	// 댓글 응답에 포함되는 작성자 요약 정보
	@Getter
	@Setter
	@NoArgsConstructor
	public static class AuthorSummary {

		private String id;
		private String nickname;
		private String handle;
		private String profileImage;
	}
}
