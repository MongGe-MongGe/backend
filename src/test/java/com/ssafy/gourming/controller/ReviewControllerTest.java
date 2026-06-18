package com.ssafy.gourming.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.gourming.config.SecurityConfig;
import com.ssafy.gourming.model.dto.PlaceDto.PlaceRequest;
import com.ssafy.gourming.model.dto.ReviewDto.AuthorSummary;
import com.ssafy.gourming.model.dto.ReviewDto.PlaceSummary;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewCreateRequest;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewPageResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewResponse;
import com.ssafy.gourming.model.dto.ReviewDto.ReviewUpdateRequest;
import com.ssafy.gourming.model.service.ReviewService;
import com.ssafy.gourming.util.JwtUtil;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@DisplayName("Review Controller test")
class ReviewControllerTest {

	private static final String USER_ID = "user-1";
	private static final String OTHER_USER_ID = "user-2";
	private static final String REVIEW_ID = "review-1";
	private static final String PLACE_ID = "place-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ReviewService reviewService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Test
	@DisplayName("creates review with authentication")
	void createReview() throws Exception {
		ReviewCreateRequest request = createCreateRequest();
		when(reviewService.createReview(any(), any())).thenReturn(createResponse());

		mockMvc.perform(post("/api/reviews")
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(REVIEW_ID))
			.andExpect(jsonPath("$.content").value("review content"))
			.andExpect(jsonPath("$.images[0]").value("/images/review-1.png"))
			.andExpect(jsonPath("$.place.id").value(PLACE_ID))
			.andExpect(jsonPath("$.author.id").value(USER_ID));

		verify(reviewService).createReview(
			eq(USER_ID),
			org.mockito.ArgumentMatchers.argThat(reviewRequest ->
				reviewRequest.getPlace().getId().equals(PLACE_ID)
					&& reviewRequest.getContent().equals("review content")
			)
		);
	}

	@Test
	@DisplayName("does not create review without authentication")
	void createReviewWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(post("/api/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createCreateRequest())))
			.andExpect(status().isUnauthorized());

		verify(reviewService, never()).createReview(any(), any());
	}

	@Test
	@DisplayName("does not create review without place")
	void createReviewWithoutPlaceFails() throws Exception {
		ReviewCreateRequest request = createCreateRequest();
		request.setPlace(null);

		mockMvc.perform(post("/api/reviews")
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(reviewService, never()).createReview(any(), any());
	}

	@Test
	@DisplayName("gets review without authentication")
	void getReview() throws Exception {
		when(reviewService.getReview(REVIEW_ID)).thenReturn(createResponse());

		mockMvc.perform(get("/api/reviews/{reviewId}", REVIEW_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(REVIEW_ID))
			.andExpect(jsonPath("$.likeCount").value(2))
			.andExpect(jsonPath("$.commentCount").value(3));

		verify(reviewService).getReview(REVIEW_ID);
	}

	@Test
	@DisplayName("updates review with authentication")
	void updateReview() throws Exception {
		ReviewUpdateRequest request = createUpdateRequest();
		when(reviewService.updateReview(any(), any(), any())).thenReturn(createResponse());

		mockMvc.perform(put("/api/reviews/{reviewId}", REVIEW_ID)
				.with(authentication(loginAuthentication()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(REVIEW_ID));

		verify(reviewService).updateReview(
			eq(USER_ID),
			eq(REVIEW_ID),
			org.mockito.ArgumentMatchers.argThat(reviewRequest ->
				reviewRequest.getContent().equals("updated content")
			)
		);
	}

	@Test
	@DisplayName("does not update review without authentication")
	void updateReviewWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(put("/api/reviews/{reviewId}", REVIEW_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createUpdateRequest())))
			.andExpect(status().isUnauthorized());

		verify(reviewService, never()).updateReview(any(), any(), any());
	}

	@Test
	@DisplayName("deletes review with authentication")
	void deleteReview() throws Exception {
		mockMvc.perform(delete("/api/reviews/{reviewId}", REVIEW_ID)
				.with(authentication(loginAuthentication())))
			.andExpect(status().isNoContent());

		verify(reviewService).deleteReview(USER_ID, REVIEW_ID);
	}

	@Test
	@DisplayName("does not delete review without authentication")
	void deleteReviewWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(delete("/api/reviews/{reviewId}", REVIEW_ID))
			.andExpect(status().isUnauthorized());

		verify(reviewService, never()).deleteReview(any(), any());
	}

	@Test
	@DisplayName("gets reviews by place without authentication")
	void getReviewsByPlace() throws Exception {
		when(reviewService.getReviewsByPlace(PLACE_ID, 0, 20))
			.thenReturn(createPageResponse());

		mockMvc.perform(get("/api/places/{placeId}/reviews", PLACE_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(REVIEW_ID))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1));

		verify(reviewService).getReviewsByPlace(PLACE_ID, 0, 20);
	}

	@Test
	@DisplayName("gets reviews by user without authentication")
	void getReviewsByUser() throws Exception {
		when(reviewService.getReviewsByUser(OTHER_USER_ID, 1, 10))
			.thenReturn(createPageResponse());

		mockMvc.perform(get("/api/users/{userId}/reviews", OTHER_USER_ID)
				.param("page", "1")
				.param("size", "10"))
			.andExpect(status().isOk());

		verify(reviewService).getReviewsByUser(OTHER_USER_ID, 1, 10);
	}

	@Test
	@DisplayName("gets my feeds with authentication")
	void getMyFeeds() throws Exception {
		when(reviewService.getMyFeeds(USER_ID, 0, 20))
			.thenReturn(createPageResponse());

		mockMvc.perform(get("/api/users/me/feeds")
				.with(authentication(loginAuthentication())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(REVIEW_ID));

		verify(reviewService).getMyFeeds(USER_ID, 0, 20);
	}

	@Test
	@DisplayName("does not get my feeds without authentication")
	void getMyFeedsWithoutAuthenticationFails() throws Exception {
		mockMvc.perform(get("/api/users/me/feeds"))
			.andExpect(status().isUnauthorized());

		verify(reviewService, never()).getMyFeeds(any(), anyInt(), anyInt());
	}

	@Test
	@DisplayName("rejects invalid page request")
	void getReviewsWithInvalidPageFails() throws Exception {
		mockMvc.perform(get("/api/places/{placeId}/reviews", PLACE_ID)
				.param("page", "-1")
				.param("size", "101"))
			.andExpect(status().isBadRequest());

		verify(reviewService, never()).getReviewsByPlace(any(), anyInt(), anyInt());
	}

	private Authentication loginAuthentication() {
		return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
	}

	private ReviewCreateRequest createCreateRequest() {
		PlaceRequest place = new PlaceRequest();
		place.setId(PLACE_ID);
		place.setName("test place");
		place.setX("127.0");
		place.setY("37.0");

		ReviewCreateRequest request = new ReviewCreateRequest();
		request.setPlace(place);
		request.setContent("review content");
		request.setImages(List.of("/images/review-1.png"));
		request.setRatingScore(5);
		request.setVisitedAt(LocalDate.of(2026, 6, 18));
		return request;
	}

	private ReviewUpdateRequest createUpdateRequest() {
		ReviewUpdateRequest request = new ReviewUpdateRequest();
		request.setContent("updated content");
		request.setImages(List.of("/images/review-2.png"));
		request.setRatingScore(4);
		request.setVisitedAt(LocalDate.of(2026, 6, 18));
		return request;
	}

	private ReviewResponse createResponse() {
		PlaceSummary place = new PlaceSummary();
		place.setId(PLACE_ID);
		place.setName("test place");
		place.setCategoryName("FD6");
		place.setRoadAddressName("test road");
		place.setX("127.0");
		place.setY("37.0");

		AuthorSummary author = new AuthorSummary();
		author.setId(USER_ID);
		author.setNickname("review user");
		author.setHandle("@review_user");
		author.setProfileImage("/images/profile.png");

		ReviewResponse response = new ReviewResponse();
		response.setId(REVIEW_ID);
		response.setContent("review content");
		response.setImages(List.of("/images/review-1.png"));
		response.setRatingScore(5);
		response.setVisitedAt(LocalDate.of(2026, 6, 18));
		response.setPlace(place);
		response.setAuthor(author);
		response.setLikeCount(2);
		response.setCommentCount(3);
		return response;
	}

	private ReviewPageResponse createPageResponse() {
		ReviewPageResponse response = new ReviewPageResponse();
		response.setContent(List.of(createResponse()));
		response.setPage(0);
		response.setSize(20);
		response.setTotalElements(1);
		response.setTotalPages(1);
		response.setFirst(true);
		response.setLast(true);
		return response;
	}
}
