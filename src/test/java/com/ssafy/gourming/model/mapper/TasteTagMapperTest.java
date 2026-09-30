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

import com.ssafy.gourming.model.dto.TasteTagDto.TasteTagRow;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("미식 태그 Mapper 테스트")
class TasteTagMapperTest {

	private static final String VERSION = "test-v1";
	private static final String ACTIVE_CODE = "test_tag_active";
	private static final String INACTIVE_CODE = "test_tag_inactive";

	@Autowired
	private TasteTagMapper tasteTagMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestData();
		jdbcTemplate.update(
			"INSERT INTO taste_tags (code, label, description, category, active, sort_order) VALUES (?, ?, ?, ?, ?, ?)",
			ACTIVE_CODE, "테스트", "테스트 설명", "MOOD", true, 999);
		jdbcTemplate.update(
			"INSERT INTO taste_tags (code, label, description, category, active, sort_order) VALUES (?, ?, ?, ?, ?, ?)",
			INACTIVE_CODE, "비활성", "비활성 설명", "MOOD", false, 999);
	}

	@AfterEach
	void tearDown() {
		deleteTestData();
	}

	@Test
	@DisplayName("활성 태그만 조회되고 벡터가 없는 태그도 포함된다")
	void selectActiveTags() {
		List<TasteTagRow> tags = tasteTagMapper.selectActiveTags();

		assertThat(tags).extracting(TasteTagRow::getCode).contains(ACTIVE_CODE).doesNotContain(INACTIVE_CODE);
		TasteTagRow active = tags.stream().filter(t -> t.getCode().equals(ACTIVE_CODE)).findFirst().get();
		assertThat(active.getEmbedding()).isEmpty();
		assertThat(active.getLabel()).isEqualTo("테스트");
		assertThat(active.getCategory()).isEqualTo("MOOD");
	}

	@Test
	@DisplayName("현재 버전 벡터가 없는 태그만 조회되고 upsert 후에는 제외된다")
	void selectTagsMissingVersionAndUpsert() {
		assertThat(tasteTagMapper.selectTagsMissingVersion(VERSION))
			.extracting(TasteTagRow::getCode).contains(ACTIVE_CODE).doesNotContain(INACTIVE_CODE);

		int changed = tasteTagMapper.upsertTagEmbedding(ACTIVE_CODE, new float[] {0.5f, 1f}, VERSION);

		assertThat(changed).isEqualTo(1);
		assertThat(tasteTagMapper.selectTagsMissingVersion(VERSION))
			.extracting(TasteTagRow::getCode).doesNotContain(ACTIVE_CODE);
		assertThat(tasteTagMapper.selectTagsMissingVersion("other-version"))
			.extracting(TasteTagRow::getCode).contains(ACTIVE_CODE);

		TasteTagRow saved = tasteTagMapper.selectActiveTags().stream()
			.filter(t -> t.getCode().equals(ACTIVE_CODE)).findFirst().get();
		assertThat(saved.getEmbedding()).containsExactly(0.5f, 1f);
		assertThat(saved.getEmbedderVersion()).isEqualTo(VERSION);
	}

	@Test
	@DisplayName("seed 태그 26개가 활성 상태로 존재한다")
	void seedTagsExist() {
		List<TasteTagRow> tags = tasteTagMapper.selectActiveTags();

		assertThat(tags).extracting(TasteTagRow::getCode)
			.contains("dessert", "quiet", "waiting", "date", "value");
		assertThat(tags.size()).isGreaterThanOrEqualTo(27); // seed 26 + 테스트 1
	}

	private void deleteTestData() {
		jdbcTemplate.update("DELETE FROM taste_tags WHERE code IN (?, ?)", ACTIVE_CODE, INACTIVE_CODE);
	}
}
