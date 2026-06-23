package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.ssafy.gourming.model.dto.RecomputeTargetDto.RecomputeTarget;
import com.ssafy.gourming.model.dto.RecomputeTargetType;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties",
	"mybatis.mapper-locations=classpath:/mappers/taste/*.xml"
})
@DisplayName("재계산 대상 Mapper 테스트")
class RecomputeTargetMapperTest {

	private static final String REVIEW_TARGET_ID = "test-recompute-review-001";
	private static final String PLACE_TARGET_ID = "test-recompute-place-001";
	private static final String USER_TARGET_ID = "test-recompute-user-001";
	private static final String FUTURE_TARGET_ID = "test-recompute-review-future";

	@Autowired
	private RecomputeTargetMapper recomputeTargetMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		createTableIfAbsent();
		deleteTestData();
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("대상을 등록하고 중복 등록 시 요청 시각을 갱신한다")
	void insertOrRefreshTarget() {
		LocalDateTime oldRequestedAt = LocalDateTime.of(2026, 6, 22, 12, 0);
		insertTarget(RecomputeTargetType.REVIEW, REVIEW_TARGET_ID, oldRequestedAt);

		int updatedCount = recomputeTargetMapper.insertOrRefreshTarget(
			RecomputeTargetType.REVIEW,
			REVIEW_TARGET_ID
		);

		RecomputeTarget selectedTarget = selectOne(
			RecomputeTargetType.REVIEW,
			REVIEW_TARGET_ID
		);
		assertThat(updatedCount).isGreaterThanOrEqualTo(1);
		assertThat(selectedTarget.getRequestedAt()).isAfter(oldRequestedAt);
	}

	@Test
	@DisplayName("대상이 없을 때만 등록하고 기존 요청 시각은 유지한다")
	void insertTargetIfAbsent() {
		LocalDateTime oldRequestedAt = LocalDateTime.of(2026, 6, 22, 12, 0);
		insertTarget(RecomputeTargetType.USER_RECOMMENDATION, USER_TARGET_ID, oldRequestedAt);

		int ignoredCount = recomputeTargetMapper.insertTargetIfAbsent(
			RecomputeTargetType.USER_RECOMMENDATION,
			USER_TARGET_ID
		);

		RecomputeTarget selectedTarget = selectOne(
			RecomputeTargetType.USER_RECOMMENDATION,
			USER_TARGET_ID
		);
		assertThat(ignoredCount).isZero();
		assertThat(selectedTarget.getRequestedAt()).isEqualToIgnoringNanos(oldRequestedAt);
	}

	@Test
	@DisplayName("cutoff 이전 대상만 조회하고 count한다")
	void selectAndCountTargetsBeforeCutoff() {
		LocalDateTime cutoffAt = LocalDateTime.of(2026, 6, 23, 0, 0);
		insertTarget(
			RecomputeTargetType.REVIEW,
			REVIEW_TARGET_ID,
			cutoffAt.minusMinutes(10)
		);
		insertTarget(
			RecomputeTargetType.REVIEW,
			FUTURE_TARGET_ID,
			cutoffAt.plusMinutes(10)
		);
		insertTarget(
			RecomputeTargetType.PLACE,
			PLACE_TARGET_ID,
			cutoffAt.minusMinutes(5)
		);

		List<RecomputeTarget> targets = recomputeTargetMapper.selectTargets(
			RecomputeTargetType.REVIEW,
			cutoffAt,
			10
		);
		long count = recomputeTargetMapper.countTargets(
			RecomputeTargetType.REVIEW,
			cutoffAt
		);

		assertThat(count).isEqualTo(1);
		assertThat(targets)
			.extracting(RecomputeTarget::getTargetId)
			.containsExactly(REVIEW_TARGET_ID);
		assertThat(targets.getFirst().getTargetType()).isEqualTo(RecomputeTargetType.REVIEW);
	}

	@Test
	@DisplayName("처리 완료 삭제는 cutoff 이후 새 요청을 보존한다")
	void deleteProcessedTargetChecksCutoff() {
		LocalDateTime cutoffAt = LocalDateTime.of(2026, 6, 23, 0, 0);
		insertTarget(
			RecomputeTargetType.REVIEW,
			REVIEW_TARGET_ID,
			cutoffAt.minusMinutes(1)
		);
		insertTarget(
			RecomputeTargetType.REVIEW,
			FUTURE_TARGET_ID,
			cutoffAt.plusMinutes(1)
		);

		int deletedCount = recomputeTargetMapper.deleteProcessedTarget(
			RecomputeTargetType.REVIEW,
			REVIEW_TARGET_ID,
			cutoffAt
		);
		int preservedCount = recomputeTargetMapper.deleteProcessedTarget(
			RecomputeTargetType.REVIEW,
			FUTURE_TARGET_ID,
			cutoffAt
		);

		assertThat(deletedCount).isEqualTo(1);
		assertThat(preservedCount).isZero();
		assertThat(selectOne(RecomputeTargetType.REVIEW, REVIEW_TARGET_ID)).isNull();
		assertThat(selectOne(RecomputeTargetType.REVIEW, FUTURE_TARGET_ID)).isNotNull();
	}

	private void createTableIfAbsent() {
		jdbcTemplate.execute(
			"""
			CREATE TABLE IF NOT EXISTS recompute_targets (
				target_type VARCHAR(30) NOT NULL,
				target_id VARCHAR(50) NOT NULL,
				requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
				PRIMARY KEY (target_type, target_id),
				KEY idx_recompute_targets_requested (requested_at, target_type)
			)
			"""
		);
	}

	private void insertTarget(
		RecomputeTargetType targetType,
		String targetId,
		LocalDateTime requestedAt
	) {
		jdbcTemplate.update(
			"""
			INSERT INTO recompute_targets (target_type, target_id, requested_at)
			VALUES (?, ?, ?)
			""",
			targetType.name(),
			targetId,
			requestedAt
		);
	}

	private RecomputeTarget selectOne(RecomputeTargetType targetType, String targetId) {
		List<RecomputeTarget> targets = recomputeTargetMapper.selectTargets(
			targetType,
			LocalDateTime.now().plusDays(1),
			100
		);

		return targets.stream()
			.filter(target -> target.getTargetId().equals(targetId))
			.findFirst()
			.orElse(null);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"""
			DELETE FROM recompute_targets
			WHERE target_id IN (?, ?, ?, ?)
			""",
			REVIEW_TARGET_ID,
			PLACE_TARGET_ID,
			USER_TARGET_ID,
			FUTURE_TARGET_ID
		);
	}
}
