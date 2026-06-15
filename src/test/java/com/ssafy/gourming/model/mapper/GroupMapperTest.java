package com.ssafy.gourming.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ssafy.gourming.model.dto.GroupDto.GroupEntity;
import com.ssafy.gourming.model.dto.GroupDto.GroupResponse;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.config.import=optional:classpath:application-local.properties"
})
@DisplayName("그룹 Mapper 테스트")
class GroupMapperTest {

	private static final String TEST_USER_ID = "test-group-user-000000000000000001";
	private static final String OTHER_USER_ID = "test-group-user-000000000000000002";

	@Autowired
	private GroupMapper groupMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		deleteTestUsers();
		insertTestUser(TEST_USER_ID, "group-user-1@test.com", "@group_user_1");
		insertTestUser(OTHER_USER_ID, "group-user-2@test.com", "@group_user_2");
	}

	@Test
	@DisplayName("그룹을 저장하고 ID로 조회한다")
	void insertGroupAndSelectGroupById() {
		GroupEntity group = createGroup(TEST_USER_ID, "친구 추천", false);

		int insertedCount = groupMapper.insertGroup(group);
		GroupEntity insertedGroup = groupMapper.selectGroupByUserIdAndName(TEST_USER_ID, "친구 추천");
		GroupEntity selectedGroup = groupMapper.selectGroupById(insertedGroup.getId());

		assertThat(insertedCount).isEqualTo(1);
		assertThat(selectedGroup).isNotNull();
		assertThat(selectedGroup.getId()).isNotBlank();
		assertThat(selectedGroup.getUserId()).isEqualTo(TEST_USER_ID);
		assertThat(selectedGroup.getName()).isEqualTo("친구 추천");
		assertThat(selectedGroup.isDefaultGroup()).isFalse();
		assertThat(selectedGroup.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("사용자의 기본 그룹을 조회한다")
	void selectDefaultGroupByUserId() {
		groupMapper.insertGroup(createGroup(TEST_USER_ID, "전체 맛집", true));
		groupMapper.insertGroup(createGroup(TEST_USER_ID, "친구 추천", false));

		GroupEntity defaultGroup = groupMapper.selectDefaultGroupByUserId(TEST_USER_ID);

		assertThat(defaultGroup).isNotNull();
		assertThat(defaultGroup.getName()).isEqualTo("전체 맛집");
		assertThat(defaultGroup.isDefaultGroup()).isTrue();
	}

	@Test
	@DisplayName("그룹 목록에서 기본 그룹을 먼저 조회한다")
	void selectGroupsByUserIdOrdersDefaultGroupFirst() {
		groupMapper.insertGroup(createGroup(TEST_USER_ID, "친구 추천", false));
		groupMapper.insertGroup(createGroup(TEST_USER_ID, "전체 맛집", true));

		List<GroupResponse> groups = groupMapper.selectGroupsByUserId(TEST_USER_ID);

		assertThat(groups).hasSize(2);
		assertThat(groups.get(0).getName()).isEqualTo("전체 맛집");
		assertThat(groups.get(0).isDefaultGroup()).isTrue();
		assertThat(groups.get(0).getGoodPlaceCount()).isZero();
		assertThat(groups.get(1).getName()).isEqualTo("친구 추천");
	}

	@Test
	@DisplayName("사용자와 이름으로 그룹을 조회한다")
	void selectGroupByUserIdAndName() {
		groupMapper.insertGroup(createGroup(TEST_USER_ID, "친구 추천", false));

		GroupEntity selectedGroup = groupMapper.selectGroupByUserIdAndName(TEST_USER_ID, "친구 추천");
		GroupEntity otherUserGroup = groupMapper.selectGroupByUserIdAndName(OTHER_USER_ID, "친구 추천");

		assertThat(selectedGroup).isNotNull();
		assertThat(selectedGroup.getId()).isNotBlank();
		assertThat(otherUserGroup).isNull();
	}

	@Test
	@DisplayName("그룹 ID로 맛집 수를 포함한 응답을 조회한다")
	void selectGroupResponseById() {
		groupMapper.insertGroup(createGroup(TEST_USER_ID, "친구 추천", false));
		String groupId =
			groupMapper.selectGroupByUserIdAndName(TEST_USER_ID, "친구 추천").getId();

		GroupResponse response =
			groupMapper.selectGroupResponseById(TEST_USER_ID, groupId);
		GroupResponse otherUserResponse =
			groupMapper.selectGroupResponseById(OTHER_USER_ID, groupId);

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(groupId);
		assertThat(response.getName()).isEqualTo("친구 추천");
		assertThat(response.getGoodPlaceCount()).isZero();
		assertThat(otherUserResponse).isNull();
	}

	@Test
	@DisplayName("소유자만 그룹 이름을 수정하고 그룹을 삭제할 수 있다")
	void updateAndDeleteGroupChecksOwner() {
		groupMapper.insertGroup(createGroup(TEST_USER_ID, "수정 전", false));
		String groupId = groupMapper.selectGroupByUserIdAndName(TEST_USER_ID, "수정 전").getId();

		int otherUserUpdatedCount = groupMapper.updateGroupName(groupId, OTHER_USER_ID, "수정 실패");
		int updatedCount = groupMapper.updateGroupName(groupId, TEST_USER_ID, "수정 후");
		int otherUserDeletedCount = groupMapper.deleteGroup(groupId, OTHER_USER_ID);
		GroupEntity updatedGroup = groupMapper.selectGroupById(groupId);
		int deletedCount = groupMapper.deleteGroup(groupId, TEST_USER_ID);

		assertThat(otherUserUpdatedCount).isZero();
		assertThat(updatedCount).isEqualTo(1);
		assertThat(otherUserDeletedCount).isZero();
		assertThat(updatedGroup.getName()).isEqualTo("수정 후");
		assertThat(deletedCount).isEqualTo(1);
		assertThat(groupMapper.selectGroupById(groupId)).isNull();
	}

	private GroupEntity createGroup(String userId, String name, boolean defaultGroup) {
		GroupEntity group = new GroupEntity();
		group.setUserId(userId);
		group.setName(name);
		group.setDefaultGroup(defaultGroup);
		return group;
	}

	private void insertTestUser(String id, String email, String handle) {
		jdbcTemplate.update(
			"""
			INSERT INTO users (id, email, password, handle, nickname)
			VALUES (?, ?, ?, ?, ?)
			""",
			id,
			email,
			"test-password",
			handle,
			"테스트 사용자"
		);
	}

	private void deleteTestUsers() {
		jdbcTemplate.update(
			"DELETE FROM users WHERE id IN (?, ?)",
			TEST_USER_ID,
			OTHER_USER_ID
		);
	}
}
