package com.ssafy.gourming.typehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("float[] JSON TypeHandler 테스트")
class FloatArrayJsonTypeHandlerTest {

	private final FloatArrayJsonTypeHandler handler = new FloatArrayJsonTypeHandler();

	@Test
	@DisplayName("float 배열을 JSON 배열 문자열로 저장한다")
	void setNonNullParameterWritesJsonArray() throws SQLException {
		PreparedStatement statement = mock(PreparedStatement.class);

		handler.setNonNullParameter(statement, 1, new float[] {0.5f, -1.25f}, null);

		verify(statement).setString(eq(1), eq("[0.5,-1.25]"));
	}

	@Test
	@DisplayName("JSON 배열 문자열을 float 배열로 읽는다")
	void getNullableResultParsesJsonArray() throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getString("embedding")).thenReturn("[0.5, -1.25, 3]");

		float[] result = handler.getNullableResult(resultSet, "embedding");

		assertThat(result).containsExactly(0.5f, -1.25f, 3f);
	}

	@Test
	@DisplayName("NULL 또는 빈 문자열은 빈 배열로 읽는다")
	void getNullableResultReturnsEmptyArrayForNull() throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.getString("embedding")).thenReturn(null);

		float[] result = handler.getNullableResult(resultSet, "embedding");

		assertThat(result).isEmpty();
	}
}
