package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.dto.PostDto;
import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.mapper.PostMapper;
import com.ssafy.gourming.model.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

	private final PostMapper postMapper;
	private final UserMapper userMapper;

	@Override
	public void createPost(String userId, PostDto.CreateRequest request) {
		// 권한 검사
		UserDto.UserEntity user = userMapper.findById(userId);
		if (user == null) {
			throw new NoSuchElementException("User not found: " + userId);
		}
		if (!"ADMIN".equals(user.getRole())) {
			throw new SecurityException("관리자만 게시글을 작성할 수 있습니다.");
		}

		// 카테고리 검증
		if (!"Notice".equals(request.getCategory()) && !"Event".equals(request.getCategory())) {
			throw new IllegalArgumentException("카테고리는 'Notice' 또는 'Event' 여야 합니다.");
		}

		String id = UUID.randomUUID().toString();
		PostDto.PostEntity postEntity = new PostDto.PostEntity(
				id,
				userId,
				request.getTitle(),
				request.getContent(),
				request.getCategory(),
				LocalDateTime.now(),
				LocalDateTime.now()
		);
		
		postMapper.insertPost(postEntity);
	}

	@Override
	public Map<String, Object> getPosts(int page, int size) {
		int offset = (page - 1) * size;
		int totalPosts = postMapper.countPosts();
		List<PostDto.PostResponse> posts = postMapper.findAllWithPaging(offset, size);
		
		int totalPages = (int) Math.ceil((double) totalPosts / size);
		
		Map<String, Object> response = new HashMap<>();
		response.put("posts", posts);
		response.put("currentPage", page);
		response.put("totalItems", totalPosts);
		response.put("totalPages", totalPages);
		
		return response;
	}

	@Override
	public PostDto.PostResponse getPostById(String id) {
		PostDto.PostResponse post = postMapper.findById(id);
		if (post == null) {
			throw new NoSuchElementException("게시글을 찾을 수 없습니다.");
		}
		return post;
	}

}
