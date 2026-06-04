package com.ssafy.gourming.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.gourming.model.dto.UserDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserMapperTest {

	@Autowired
	private UserMapper userMapper;

	private String uid() {
		return String.valueOf(System.currentTimeMillis());
	}

	@Test
	@DisplayName("[Mapper] INSERT 후 findByEmail 조회 성공")
	void insertAndFindByEmail() {
		String email  = "map_" + uid() + "@test.com";
		String handle = "@mapper_" + uid();

		UserDto.SignupRequest req = new UserDto.SignupRequest();
		ReflectionTestUtils.setField(req, "email",    email);
		ReflectionTestUtils.setField(req, "password", "$2a$12$hashedPasswordForTest");
		ReflectionTestUtils.setField(req, "nickname", "매퍼테스트유저");
		ReflectionTestUtils.setField(req, "handle",   handle);
		ReflectionTestUtils.setField(req, "phone",    "010-0000-0001");

		try {
			userMapper.insertUser(req);
		} catch (Exception e) {
			log.error("[insertUser 실패] {}: {}", e.getClass().getSimpleName(), e.getMessage());
			fail("insertUser 실패: " + e.getMessage());
		}

		UserDto.UserEntity result = null;
		try {
			result = userMapper.findByEmail(email);
		} catch (Exception e) {
			log.error("[findByEmail 실패] {}: {}", e.getClass().getSimpleName(), e.getMessage());
			fail("findByEmail 실패: " + e.getMessage());
		}

		assertNotNull(result, "findByEmail → null (삽입 후 조회 실패)");
		assertEquals(email,         result.getEmail(),    "email 불일치");
		assertEquals("매퍼테스트유저", result.getNickname(), "nickname 불일치");
		assertEquals(handle,        result.getHandle(),   "handle 불일치");
		assertNotNull(result.getId(), "id null → UUID 자동 생성 실패");
	}

	@Test
	@DisplayName("[Mapper] 없는 이메일 조회 → null 반환")
	void findByEmail_notFound() {
		try {
			UserDto.UserEntity result = userMapper.findByEmail("ghost_" + uid() + "@test.com");
			assertNull(result, "존재하지 않는 이메일인데 null이 아님");
		} catch (Exception e) {
			log.error("[findByEmail_notFound 실패] {}: {}", e.getClass().getSimpleName(), e.getMessage());
			fail("예외 발생: " + e.getMessage());
		}
	}

	@Test
	@DisplayName("[Mapper] handle로 사용자 조회 성공")
	void findByHandle_success() {
		String handle = "@h_" + uid();

		UserDto.SignupRequest req = new UserDto.SignupRequest();
		ReflectionTestUtils.setField(req, "email",    "h_" + uid() + "@test.com");
		ReflectionTestUtils.setField(req, "password", "$2a$12$hashedPw");
		ReflectionTestUtils.setField(req, "nickname", "핸들테스트유저");
		ReflectionTestUtils.setField(req, "handle",   handle);

		try {
			userMapper.insertUser(req);
		} catch (Exception e) {
			log.error("[findByHandle_success - insertUser 실패] {}: {}", e.getClass().getSimpleName(), e.getMessage());
			fail("사전 INSERT 실패: " + e.getMessage());
		}

		try {
			UserDto.UserEntity result = userMapper.findByHandle(handle);
			assertNotNull(result,  "findByHandle → null (조회 실패)");
			assertEquals(handle,           result.getHandle(),   "handle 불일치");
			assertEquals("핸들테스트유저", result.getNickname(), "nickname 불일치");
		} catch (AssertionError e) {
			throw e; // assert 실패는 그대로 전파
		} catch (Exception e) {
			log.error("[findByHandle 실패] {}: {}", e.getClass().getSimpleName(), e.getMessage());
			fail("findByHandle 실패: " + e.getMessage());
		}
	}

	@Test
	@DisplayName("[Mapper] 없는 handle 조회 → null 반환")
	void findByHandle_notFound() {
		try {
			UserDto.UserEntity result = userMapper.findByHandle("@nobody_ever_" + uid());
			assertNull(result, "존재하지 않는 handle인데 null이 아님");
		} catch (Exception e) {
			log.error("[findByHandle_notFound 실패] {}: {}", e.getClass().getSimpleName(), e.getMessage());
			fail("예외 발생: " + e.getMessage());
		}
	}
}
