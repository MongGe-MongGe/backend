package com.ssafy.gourming.typehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Map<String, Double> JSON TypeHandler 테스트")
class MapJsonTypeHandlerTest {

	private final MapJsonTypeHandler handler = new MapJsonTypeHandler();

	@Test
	@DisplayName("Map을 JSON 객체 문자열로 저장한다")
	void setNonNullParameterWritesJsonObject() throws SQLException {
		PreparedStatement statement = mock(PreparedStatement.class);
		Map<String, Double> map = new LinkedHashMap<>();
		map.put("dessert", 0.9);
		map.put("waiting", -0.8);

		handler.setNonNullParameter(statement, 1, map, null);

		verify(statement).setString(eq(1), eq("{\"dessert\":0.9,\"waiting\":-0.8}"));
	}

	@Test
	@DisplayName("JSON 객체 문자열을 Map으로 읽는다")
	void getNullableResultParsesJsonObject() throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getString("tag_sentiments")).thenReturn("{\"dessert\": 0.9, \"waiting\": -0.8}");

		Map<String, Double> result = handler.getNullableResult(resultSet, "tag_sentiments");

		assertThat(result).containsEntry("dessert", 0.9).containsEntry("waiting", -0.8);
	}

	@Test
	@DisplayName("NULL은 빈 Map으로 읽는다")
	void getNullableResultReturnsEmptyMapForNull() throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getString("tag_sentiments")).thenReturn(null);

		assertThat(handler.getNullableResult(resultSet, "tag_sentiments")).isEmpty();
	}
}
