package com.ssafy.gourming.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.gourming.model.dto.PostDto;

@Mapper
public interface PostMapper {
	
	// 게시글 작성
	void insertPost(PostDto.PostEntity post);
	
	// 전체 게시글 개수 조회 (페이지네이션용)
	int countPosts();
	
	// 페이지네이션을 적용하여 게시글 목록 조회
	List<PostDto.PostListResponse> findAllWithPaging(@Param("offset") int offset, @Param("limit") int limit);
	
	// 단일 게시글 상세 조회
	PostDto.PostResponse findById(String id);
}
