package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.ssafy.gourming.model.dto.TasteTagDto.TasteTag;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties",
	"mybatis.mapper-locations=classpath:/mappers/taste/*.xml"
})
@DisplayName("미식 태그 Mapper 테스트")
class TasteTagMapperTest {

	private static final String ACTIVE_CODE = "test_mapper_quiet";
	private static final String SECOND_ACTIVE_CODE = "test_mapper_dessert";
	private static final String INACTIVE_CODE = "test_mapper_inactive";

	@Autowired
	private TasteTagMapper tasteTagMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		createTableIfAbsent();
		deleteTestData();
		insertTasteTag(ACTIVE_CODE, "조용함", "MOOD", true);
		insertTasteTag(SECOND_ACTIVE_CODE, "디저트", "CATEGORY", true);
		insertTasteTag(INACTIVE_CODE, "비활성 태그", "MOOD", false);
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("활성 미식 태그만 조회한다")
	void selectActiveTags() {
		List<TasteTag> activeTags = tasteTagMapper.selectActiveTags();

		assertThat(activeTags)
			.extracting(TasteTag::getCode)
			.contains(ACTIVE_CODE, SECOND_ACTIVE_CODE)
			.doesNotContain(INACTIVE_CODE);
		assertThat(activeTags).allMatch(TasteTag::isActive);
	}

	@Test
	@DisplayName("code 목록으로 미식 태그를 조회한다")
	void selectTagsByCodes() {
		List<TasteTag> tags = tasteTagMapper.selectTagsByCodes(
			List.of(ACTIVE_CODE, SECOND_ACTIVE_CODE)
		);

		assertThat(tags)
			.extracting(TasteTag::getCode)
			.containsExactlyInAnyOrder(ACTIVE_CODE, SECOND_ACTIVE_CODE);
		assertThat(tags)
			.allSatisfy(tag -> {
				assertThat(tag.getId()).isNotNull();
				assertThat(tag.getName()).isNotBlank();
				assertThat(tag.getType()).isNotBlank();
				assertThat(tag.getCreatedAt()).isNotNull();
				assertThat(tag.getUpdatedAt()).isNotNull();
			});
	}

	private void createTableIfAbsent() {
		jdbcTemplate.execute(
			"""
			CREATE TABLE IF NOT EXISTS taste_tags (
				id BIGINT NOT NULL AUTO_INCREMENT,
				code VARCHAR(50) NOT NULL,
				name VARCHAR(50) NOT NULL,
				type VARCHAR(30) NOT NULL,
				description VARCHAR(255),
				active BOOLEAN NOT NULL DEFAULT TRUE,
				created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
				updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
				PRIMARY KEY (id),
				UNIQUE KEY uq_taste_tags_code (code)
			)
			"""
		);
	}

	private void insertTasteTag(String code, String name, String type, boolean active) {
		jdbcTemplate.update(
			"""
			INSERT INTO taste_tags (code, name, type, description, active)
			VALUES (?, ?, ?, ?, ?)
			""",
			code,
			name,
			type,
			"mapper test tag",
			active
		);
	}

	private void deleteTestData() {
		jdbcTemplate.update(
			"""
			DELETE FROM taste_tags
			WHERE code IN (?, ?, ?)
			""",
			ACTIVE_CODE,
			SECOND_ACTIVE_CODE,
			INACTIVE_CODE
		);
	}
}
