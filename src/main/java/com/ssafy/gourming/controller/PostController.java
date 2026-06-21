package com.ssafy.gourming.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.PostDto;
import com.ssafy.gourming.model.service.PostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@Validated
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<PostDto.CreateResponse> createPost(
			@AuthenticationPrincipal String userId,
			@Validated @RequestBody PostDto.CreateRequest request
	) {
		String postId = postService.createPost(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new PostDto.CreateResponse(postId));
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getPosts(
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size
	) {
		return ResponseEntity.ok(postService.getPosts(page, size));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PostDto.PostResponse> getPostById(@PathVariable String id) {
		return ResponseEntity.ok(postService.getPostById(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> updatePost(
			@PathVariable String id,
			@Validated @RequestBody PostDto.UpdateRequest request
	) {
		postService.updatePost(id, request);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deletePost(@PathVariable String id) {
		postService.deletePost(id);
		return ResponseEntity.ok().build();
	}
}
