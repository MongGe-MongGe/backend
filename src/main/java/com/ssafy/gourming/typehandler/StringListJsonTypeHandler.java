package com.ssafy.gourming.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// 리뷰 이미지 URL 목록을 DB의 JSON 문자열 컬럼과 Java List<String> 사이에서 변환한다.
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<List<String>> STRING_LIST_TYPE =
		new TypeReference<>() {
		};

	@Override
	public void setNonNullParameter(
		PreparedStatement ps,
		int i,
		List<String> parameter,
		JdbcType jdbcType
	) throws SQLException {
		try {
			ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
		} catch (JsonProcessingException exception) {
			throw new SQLException("Failed to serialize string list to JSON", exception);
		}
	}

	@Override
	public List<String> getNullableResult(ResultSet rs, String columnName)
		throws SQLException {
		return parseJson(rs.getString(columnName));
	}

	@Override
	public List<String> getNullableResult(ResultSet rs, int columnIndex)
		throws SQLException {
		return parseJson(rs.getString(columnIndex));
	}

	@Override
	public List<String> getNullableResult(CallableStatement cs, int columnIndex)
		throws SQLException {
		return parseJson(cs.getString(columnIndex));
	}

	private List<String> parseJson(String json) throws SQLException {
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}
		try {
			return OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
		} catch (JsonProcessingException exception) {
			throw new SQLException("Failed to deserialize JSON to string list", exception);
		}
	}
}
