package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.PlaceReviewSummaryEntity;
import com.ssafy.gourming.model.dto.PlaceReviewSummaryDto.ReviewSummarySourceRow;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("장소 리뷰 요약 Mapper 테스트")
class PlaceReviewSummaryMapperTest {

	private static final String USER_ID = "95000000-0000-0000-0000-000000000001";
	private static final String PLACE_ID = "test-place-summary-001";
	private static final String OTHER_PLACE_ID = "test-place-summary-002";
	private static final String REVIEW_ID = "96000000-0000-0000-0000-000000000001";
	private static final String OTHER_REVIEW_ID = "96000000-0000-0000-0000-000000000002";

	@Autowired
	private PlaceReviewSummaryMapper placeReviewSummaryMapper;

	@Autowired
	private PlaceMapper placeMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		insertTestUser();
		insertTestPlace(PLACE_ID, "Summary Test Place");
		insertTestPlace(OTHER_PLACE_ID, "Other Summary Test Place");
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("장소 리뷰 요약을 저장한 뒤 조회한다")
	void upsertAndSelectByPlaceId() {
		PlaceReviewSummaryEntity summary = createSummary(
			PLACE_ID,
			"리뷰 요약입니다.",
			"COMPLETED"
		);

		int changedCount = placeReviewSummaryMapper.upsert(summary);
		PlaceReviewSummaryEntity selectedSummary =
			placeReviewSummaryMapper.selectByPlaceId(PLACE_ID);

		assertThat(changedCount).isGreaterThanOrEqualTo(1);
		assertThat(selectedSummary).isNotNull();
		assertThat(selectedSummary.getPlaceId()).isEqualTo(PLACE_ID);
		assertThat(selectedSummary.getSummary()).isEqualTo("리뷰 요약입니다.");
		assertThat(selectedSummary.getPositivePoints())
			.containsExactly("맛이 좋아요", "분위기가 좋아요");
		assertThat(selectedSummary.getNegativePoints())
			.containsExactly("대기가 길 수 있어요");
		assertThat(selectedSummary.getRecommendedFor())
			.containsExactly("데이트", "친구 모임");
		assertThat(selectedSummary.getKeywords())
			.containsExactly("파스타", "분위기", "친절");
		assertThat(selectedSummary.getReviewCount()).isEqualTo(2);
		assertThat(selectedSummary.getModelVersion()).isEqualTo("test-model");
		assertThat(selectedSummary.getStatus()).isEqualTo("COMPLETED");
		assertThat(selectedSummary.getErrorMessage()).isNull();
		assertThat(selectedSummary.getCreatedAt()).isNotNull();
		assertThat(selectedSummary.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("같은 placeId로 다시 저장하면 요약이 갱신된다")
	void upsertUpdatesExistingSummary() {
		placeReviewSummaryMapper.upsert(createSummary(
			PLACE_ID,
			"기존 요약입니다.",
			"COMPLETED"
		));

		PlaceReviewSummaryEntity updatedSummary = createSummary(
			PLACE_ID,
			"새 요약입니다.",
			"COMPLETED"
		);
		updatedSummary.setPositivePoints(List.of("새로운 장점"));
		updatedSummary.setNegativePoints(List.of());
		updatedSummary.setKeywords(List.of("새키워드"));
		updatedSummary.setReviewCount(3);

		int changedCount = placeReviewSummaryMapper.upsert(updatedSummary);
		PlaceReviewSummaryEntity selectedSummary =
			placeReviewSummaryMapper.selectByPlaceId(PLACE_ID);

		assertThat(changedCount).isGreaterThanOrEqualTo(1);
		assertThat(selectedSummary.getSummary()).isEqualTo("새 요약입니다.");
		assertThat(selectedSummary.getPositivePoints()).containsExactly("새로운 장점");
		assertThat(selectedSummary.getNegativePoints()).isEmpty();
		assertThat(selectedSummary.getKeywords()).containsExactly("새키워드");
		assertThat(selectedSummary.getReviewCount()).isEqualTo(3);
	}

	@Test
	@DisplayName("장소 리뷰 요약을 PROCESSING 상태로 표시한다")
	void markProcessing() {
		int changedCount = placeReviewSummaryMapper.markProcessing(
			PLACE_ID,
			"test-model"
		);
		PlaceReviewSummaryEntity selectedSummary =
			placeReviewSummaryMapper.selectByPlaceId(PLACE_ID);

		assertThat(changedCount).isGreaterThanOrEqualTo(1);
		assertThat(selectedSummary).isNotNull();
		assertThat(selectedSummary.getStatus()).isEqualTo("PROCESSING");
		assertThat(selectedSummary.getModelVersion()).isEqualTo("test-model");
		assertThat(selectedSummary.getPositivePoints()).isEmpty();
		assertThat(selectedSummary.getNegativePoints()).isEmpty();
		assertThat(selectedSummary.getRecommendedFor()).isEmpty();
		assertThat(selectedSummary.getKeywords()).isEmpty();
		assertThat(selectedSummary.getErrorMessage()).isNull();
	}

	@Test
	@DisplayName("장소 리뷰 요약을 FAILED 상태로 표시한다")
	void markFailed() {
		int changedCount = placeReviewSummaryMapper.markFailed(
			PLACE_ID,
			"test-model",
			"AI 응답 파싱 실패"
		);
		PlaceReviewSummaryEntity selectedSummary =
			placeReviewSummaryMapper.selectByPlaceId(PLACE_ID);

		assertThat(changedCount).isGreaterThanOrEqualTo(1);
		assertThat(selectedSummary).isNotNull();
		assertThat(selectedSummary.getStatus()).isEqualTo("FAILED");
		assertThat(selectedSummary.getModelVersion()).isEqualTo("test-model");
		assertThat(selectedSummary.getErrorMessage()).isEqualTo("AI 응답 파싱 실패");
	}

	@Test
	@DisplayName("장소의 리뷰 요약 원본 목록을 최신순으로 조회한다")
	void selectReviewSourcesByPlaceId() {
		insertTestReview(REVIEW_ID, PLACE_ID, "첫 번째 리뷰", LocalDateTime.of(2026, 6, 24, 10, 0));
		insertTestReview(OTHER_REVIEW_ID, PLACE_ID, "두 번째 리뷰", LocalDateTime.of(2026, 6, 24, 11, 0));

		List<ReviewSummarySourceRow> reviewSources =
			placeReviewSummaryMapper.selectReviewSourcesByPlaceId(PLACE_ID);

		assertThat(reviewSources).hasSize(2);
		assertThat(reviewSources)
			.extracting(ReviewSummarySourceRow::getReviewId)
			.containsExactly(OTHER_REVIEW_ID, REVIEW_ID);
		assertThat(reviewSources.get(0).getContent()).isEqualTo("두 번째 리뷰");
		assertThat(reviewSources.get(0).getRatingScore()).isEqualTo(5);
		assertThat(reviewSources.get(0).getVisitedAt()).isEqualTo(LocalDate.of(2026, 6, 24));
		assertThat(reviewSources.get(0).getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 24, 11, 0));
	}

	@Test
	@DisplayName("전체 장소 ID 목록을 생성순으로 조회한다")
	void selectAllPlaceIds() {
		List<String> placeIds = placeMapper.selectAllPlaceIds();

		assertThat(placeIds).contains(PLACE_ID, OTHER_PLACE_ID);
	}

	@Test
	@DisplayName("장소 삭제 시 리뷰 요약도 함께 삭제된다")
	void deletePlaceCascadesSummary() {
		placeReviewSummaryMapper.upsert(createSummary(
			PLACE_ID,
			"삭제될 요약입니다.",
			"COMPLETED"
		));

		placeMapper.deletePlaceById(PLACE_ID);
		PlaceReviewSummaryEntity selectedSummary =
			placeReviewSummaryMapper.selectByPlaceId(PLACE_ID);

		assertThat(selectedSummary).isNull();
	}

	private PlaceReviewSummaryEntity createSummary(
		String placeId,
		String summaryText,
		String status
	) {
		PlaceReviewSummaryEntity summary = new PlaceReviewSummaryEntity();
		summary.setPlaceId(placeId);
		summary.setSummary(summaryText);
		summary.setPositivePoints(List.of("맛이 좋아요", "분위기가 좋아요"));
		summary.setNegativePoints(List.of("대기가 길 수 있어요"));
		summary.setRecommendedFor(List.of("데이트", "친구 모임"));
		summary.setKeywords(List.of("파스타", "분위기", "친절"));
		summary.setReviewCount(2);
		summary.setModelVersion("test-model");
		summary.setStatus(status);
		summary.setLastReviewUpdatedAt(LocalDateTime.of(2026, 6, 24, 12, 0));
		return summary;
	}

	private void insertTestUser() {
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, email, password, handle, nickname)
			VALUES (?, ?, ?, ?, ?)
			""",
			USER_ID,
			"summary-user@test.com",
			"test-password",
			"@summary_user",
			"Summary Test User"
		);
	}

	private void insertTestPlace(String id, String name) {
		jdbcTemplate.update(
			"""
			INSERT INTO places (
				id, name, category_name, category_group_code, road_address_name, x, y
			)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""",
			id,
			name,
			"음식점 > 양식",
			"FD6",
			"Seoul Summary Test Road 1",
			"127.000000",
			"37.000000"
		);
	}

	private void insertTestReview(
		String id,
		String placeId,
		String content,
		LocalDateTime createdAt
	) {
		jdbcTemplate.update(
			"""
			INSERT INTO reviews (
				id, content, images, rating_score, visited_at, place_id, user_id, created_at, updated_at
			)
			VALUES (?, ?, JSON_ARRAY(), ?, ?, ?, ?, ?, ?)
			""",
			id,
			content,
			5,
			LocalDate.of(2026, 6, 24),
			placeId,
			USER_ID,
			createdAt,
			createdAt
		);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"DELETE FROM place_review_summaries WHERE place_id IN (?, ?)",
			PLACE_ID,
			OTHER_PLACE_ID
		);
		jdbcTemplate.update(
			"DELETE FROM reviews WHERE id IN (?, ?)",
			REVIEW_ID,
			OTHER_REVIEW_ID
		);
		jdbcTemplate.update(
			"DELETE FROM places WHERE id IN (?, ?)",
			PLACE_ID,
			OTHER_PLACE_ID
		);
		jdbcTemplate.update(
			"DELETE FROM users WHERE id = ?",
			USER_ID
		);
	}
}
