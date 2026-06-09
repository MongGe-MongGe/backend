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
import com.ssafy.gourming.security.LoginUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class GroupController {

	private final GroupService groupService;

	@GetMapping("/me/groups")
	public ResponseEntity<List<GroupResponse>> getMyGroups(
		@AuthenticationPrincipal LoginUser loginUser
	) {
		return ResponseEntity.ok(groupService.getMyGroups(loginUser.id()));
	}

	@GetMapping("/{userId}/groups")
	public ResponseEntity<List<GroupResponse>> getUserGroups(
		@PathVariable String userId
	) {
		return ResponseEntity.ok(groupService.getUserGroups(userId));
	}

	@PostMapping("/me/groups")
	public ResponseEntity<GroupResponse> createGroup(
		@AuthenticationPrincipal LoginUser loginUser,
		@Valid @RequestBody GroupCreateRequest request
	) {
		GroupResponse response = groupService.createGroup(loginUser.id(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/me/groups/{groupId}")
	public ResponseEntity<GroupResponse> updateGroup(
		@AuthenticationPrincipal LoginUser loginUser,
		@PathVariable String groupId,
		@Valid @RequestBody GroupUpdateRequest request
	) {
		return ResponseEntity.ok(groupService.updateGroup(loginUser.id(), groupId, request));
	}

	@DeleteMapping("/me/groups/{groupId}")
	public ResponseEntity<Void> deleteGroup(
		@AuthenticationPrincipal LoginUser loginUser,
		@PathVariable String groupId
	) {
		groupService.deleteGroup(loginUser.id(), groupId);
		return ResponseEntity.noContent().build();
	}
}
