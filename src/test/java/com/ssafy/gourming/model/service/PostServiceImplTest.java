package com.ssafy.gourming.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.gourming.model.dto.PostDto;
import com.ssafy.gourming.model.dto.PostCategory;
import com.ssafy.gourming.model.mapper.PostMapper;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    @DisplayName("게시글 생성 테스트")
    void testCreatePost() {
        // given
        String userId = "user1";
        PostDto.CreateRequest request = new PostDto.CreateRequest();
        request.setTitle("Test Title");
        request.setContent("Test Content");
        request.setCategory(PostCategory.Notice);

        // when
        String postId = postService.createPost(userId, request);

        // then
        assertNotNull(postId);
        verify(postMapper, times(1)).insertPost(any(PostDto.PostEntity.class));
    }

    @Test
    @DisplayName("게시글 페이징 조회 테스트 (0-based)")
    void testGetPosts() {
        // given
        int page = 0;
        int size = 10;

        List<PostDto.PostListResponse> mockList = Arrays.asList(
                new PostDto.PostListResponse("id1", "Title 1", "Notice", LocalDateTime.now(), LocalDateTime.now(),
                        "user1", "author1"),
                new PostDto.PostListResponse("id2", "Title 2", "Event", LocalDateTime.now(), LocalDateTime.now(),
                        "user2", "author2"));

        when(postMapper.countPosts()).thenReturn(2);
        when(postMapper.findAllWithPaging(0, 10)).thenReturn(mockList);

        // when
        Map<String, Object> result = postService.getPosts(page, size);

        // then
        assertEquals(0, result.get("currentPage"));
        assertEquals(2, result.get("totalItems"));
        assertEquals(1, result.get("totalPages")); // 2 items / 10 size = 1 page
        assertEquals(mockList, result.get("posts"));
    }

    @Test
    @DisplayName("게시글 단건 조회 성공 테스트")
    void testGetPostById_Success() {
        // given
        String postId = "id1";
        PostDto.PostResponse mockPost = new PostDto.PostResponse(postId, "Title 1", "Content 1", "Notice",
                LocalDateTime.now(), LocalDateTime.now(), "user1", "author1");
        when(postMapper.findById(postId)).thenReturn(mockPost);

        // when
        PostDto.PostResponse result = postService.getPostById(postId);

        // then
        assertNotNull(result);
        assertEquals("Title 1", result.getTitle());
    }

    @Test
    @DisplayName("게시글 단건 조회 실패 테스트 (존재하지 않음)")
    void testGetPostById_NotFound() {
        // given
        String postId = "id1";
        when(postMapper.findById(postId)).thenReturn(null);

        // when & then
        assertThrows(NoSuchElementException.class, () -> postService.getPostById(postId));
    }

    @Test
    @DisplayName("게시글 수정 성공 테스트")
    void testUpdatePost_Success() {
        // given
        String postId = "id1";
        PostDto.UpdateRequest request = new PostDto.UpdateRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated Content");
        request.setCategory(PostCategory.Event);

        PostDto.PostResponse mockPost = new PostDto.PostResponse(postId, "Title 1", "Content 1", "Notice",
                LocalDateTime.now(), LocalDateTime.now(), "user1", "author1");
        when(postMapper.findById(postId)).thenReturn(mockPost);

        // when
        postService.updatePost(postId, request);

        // then
        verify(postMapper, times(1)).updatePost(any(PostDto.PostEntity.class));
    }

    @Test
    @DisplayName("게시글 수정 실패 테스트 (존재하지 않음)")
    void testUpdatePost_NotFound() {
        // given
        String postId = "id1";
        PostDto.UpdateRequest request = new PostDto.UpdateRequest();
        when(postMapper.findById(postId)).thenReturn(null);

        // when & then
        assertThrows(NoSuchElementException.class, () -> postService.updatePost(postId, request));
    }

    @Test
    @DisplayName("게시글 삭제 성공 테스트")
    void testDeletePost_Success() {
        // given
        String postId = "id1";
        PostDto.PostResponse mockPost = new PostDto.PostResponse(postId, "Title 1", "Content 1", "Notice",
                LocalDateTime.now(), LocalDateTime.now(), "user1", "author1");
        when(postMapper.findById(postId)).thenReturn(mockPost);

        // when
        postService.deletePost(postId);

        // then
        verify(postMapper, times(1)).deletePost(postId);
    }
}
