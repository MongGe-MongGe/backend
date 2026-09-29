package com.ssafy.gourming.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// 임베딩 벡터를 DB의 JSON 배열 컬럼과 Java float[] 사이에서 변환한다.
public class FloatArrayJsonTypeHandler extends BaseTypeHandler<float[]> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public void setNonNullParameter(
		PreparedStatement ps,
		int i,
		float[] parameter,
		JdbcType jdbcType
	) throws SQLException {
		try {
			ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
		} catch (JsonProcessingException exception) {
			throw new SQLException("Failed to serialize float array to JSON", exception);
		}
	}

	@Override
	public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return parseJson(rs.getString(columnName));
	}

	@Override
	public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return parseJson(rs.getString(columnIndex));
	}

	@Override
	public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return parseJson(cs.getString(columnIndex));
	}

	private float[] parseJson(String json) throws SQLException {
		if (json == null || json.isBlank()) {
			return new float[0];
		}
		try {
			return OBJECT_MAPPER.readValue(json, float[].class);
		} catch (JsonProcessingException exception) {
			throw new SQLException("Failed to deserialize JSON to float array", exception);
		}
	}
}
