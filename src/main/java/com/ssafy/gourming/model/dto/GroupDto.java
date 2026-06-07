package com.ssafy.gourming.model.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class GroupDto {

	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupEntity {

		private String id;
		private String userId;
		private String name;
		private boolean defaultGroup;
		private LocalDateTime createdAt;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupCreateRequest {

		@NotBlank(message = "그룹 이름은 필수입니다.")
		@Size(max = 100, message = "그룹 이름은 100자 이하여야 합니다.")
		private String name;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupUpdateRequest {

		@NotBlank(message = "그룹 이름은 필수입니다.")
		@Size(max = 100, message = "그룹 이름은 100자 이하여야 합니다.")
		private String name;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupResponse {

		private String id;
		private String name;
		private boolean defaultGroup;
		private int goodPlaceCount;
		private LocalDateTime createdAt;
	}
}
