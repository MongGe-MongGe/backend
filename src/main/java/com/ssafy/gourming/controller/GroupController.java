package com.ssafy.gourming.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.GroupDto.GroupCreateRequest;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;
import com.ssafy.gourming.model.dto.GroupDto.GroupUpdateRequest;
import com.ssafy.gourming.model.service.GroupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class GroupController {

	private final GroupService groupService;

	// JWT 필터가 SecurityContext에 등록한 사용자 ID를 사용한다.
	@GetMapping("/me/groups")
	public ResponseEntity<List<GroupResponse>> getMyGroups(
		@AuthenticationPrincipal String userId
	) {
		return ResponseEntity.ok(groupService.getGroupsByUserId(userId));
	}

	@GetMapping("/{userId}/groups")
	public ResponseEntity<List<GroupResponse>> getUserGroups(
		@PathVariable String userId
	) {
		return ResponseEntity.ok(groupService.getGroupsByUserId(userId));
	}

	@PostMapping("/me/groups")
	public ResponseEntity<GroupResponse> createGroup(
		@AuthenticationPrincipal String userId,
		@Valid @RequestBody GroupCreateRequest request
	) {
		GroupResponse response = groupService.createGroup(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/me/groups/{groupId}")
	public ResponseEntity<GroupResponse> updateGroup(
		@AuthenticationPrincipal String userId,
		@PathVariable String groupId,
		@Valid @RequestBody GroupUpdateRequest request
	) {
		return ResponseEntity.ok(groupService.updateGroup(userId, groupId, request));
	}

	@DeleteMapping("/me/groups/{groupId}")
	public ResponseEntity<Void> deleteGroup(
		@AuthenticationPrincipal String userId,
		@PathVariable String groupId
	) {
		groupService.deleteGroup(userId, groupId);
		return ResponseEntity.noContent().build();
	}
}
