package com.ssafy.gourming.model.client;

import java.util.List;

public interface TextEmbedder {

	// 텍스트 목록을 같은 순서의 벡터 목록으로 변환한다. 빈 입력이면 빈 목록을 반환한다.
	List<float[]> embed(List<String> texts);

	// 저장된 벡터가 어떤 모델로 만들어졌는지 구분하는 버전 문자열이다.
	String version();
}
