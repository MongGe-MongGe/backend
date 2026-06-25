package com.ssafy.gourming.model.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ssafy.gourming.model.dto.PostDto;
import com.ssafy.gourming.model.mapper.PostMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

	private final PostMapper postMapper;

	@Override
	public String createPost(String userId, PostDto.CreateRequest request) {
		String id = UUID.randomUUID().toString();
		PostDto.PostEntity postEntity = new PostDto.PostEntity(
				id,
				userId,
				request.getTitle(),
				request.getContent(),
				request.getCategory().name(),
				LocalDateTime.now(),
				LocalDateTime.now());

		postMapper.insertPost(postEntity);
		return id;
	}

	@Override
	public Map<String, Object> getPosts(int page, int size) {
		int offset = page * size;
		int totalPosts = postMapper.countPosts();
		List<PostDto.PostListResponse> posts = postMapper.findAllWithPaging(offset, size);

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

	@Override
	public void updatePost(String id, PostDto.UpdateRequest request) {
		PostDto.PostResponse post = postMapper.findById(id);
		if (post == null) {
			throw new NoSuchElementException("게시글을 찾을 수 없습니다.");
		}

		PostDto.PostEntity entity = new PostDto.PostEntity(
				id,
				post.getUserId(),
				request.getTitle(),
				request.getContent(),
				request.getCategory().name(),
				post.getCreatedAt(),
				LocalDateTime.now());
		postMapper.updatePost(entity);
	}

	@Override
	public void deletePost(String id) {
		PostDto.PostResponse post = postMapper.findById(id);
		if (post == null) {
			throw new NoSuchElementException("게시글을 찾을 수 없습니다.");
		}
		postMapper.deletePost(id);
	}

}
