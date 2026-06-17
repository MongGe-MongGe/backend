package com.ssafy.gourming.model.service;

import java.util.Map;

import com.ssafy.gourming.model.dto.PostDto;

public interface PostService {
	
	// 게시글 작성
	void createPost(String userId, PostDto.CreateRequest request);
	
	// 게시글 목록 페이징 조회
	Map<String, Object> getPosts(int page, int size);
	
	// 단일 게시글 조회
	PostDto.PostResponse getPostById(String id);
}
