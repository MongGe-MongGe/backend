package com.ssafy.gourming.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// 태그별 점수 Map을 DB의 JSON 객체 컬럼과 Java Map<String, Double> 사이에서 변환한다.
public class MapJsonTypeHandler extends BaseTypeHandler<Map<String, Double>> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<LinkedHashMap<String, Double>> MAP_TYPE =
		new TypeReference<>() {
		};

	@Override
	public void setNonNullParameter(
		PreparedStatement ps,
		int i,
		Map<String, Double> parameter,
		JdbcType jdbcType
	) throws SQLException {
		try {
			ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
		} catch (JsonProcessingException exception) {
			throw new SQLException("Failed to serialize map to JSON", exception);
		}
	}

	@Override
	public Map<String, Double> getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return parseJson(rs.getString(columnName));
	}

	@Override
	public Map<String, Double> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return parseJson(rs.getString(columnIndex));
	}

	@Override
	public Map<String, Double> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return parseJson(cs.getString(columnIndex));
	}

	private Map<String, Double> parseJson(String json) throws SQLException {
		if (json == null || json.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			return OBJECT_MAPPER.readValue(json, MAP_TYPE);
		} catch (JsonProcessingException exception) {
			throw new SQLException("Failed to deserialize JSON to map", exception);
		}
	}
}
