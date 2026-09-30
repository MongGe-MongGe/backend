package com.ssafy.gourming.model.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 외부 API 없이 키워드 등장 횟수로 벡터를 만든다.
// 각 차원은 태그 하나에 대응하므로 테스트에서 코사인 결과를 예측할 수 있다.
@Component
@ConditionalOnProperty(
	name = "embedding.ai.enabled",
	havingValue = "false",
	matchIfMissing = true
)
public class FakeTextEmbedder implements TextEmbedder {

	public static final String VERSION = "fake-v1";

	// {태그 코드, 키워드...}. 태그 설명문에도 같은 키워드가 들어가야 태그 벡터가 만들어진다.
	private static final String[][] KEYWORDS = {
		{"cafe", "카페", "커피"},
		{"dessert", "디저트", "케이크", "빵"},
		{"bakery", "베이커리", "빵집"},
		{"korean", "한식", "국밥", "김치"},
		{"japanese", "일식", "라멘", "초밥"},
		{"western", "양식", "파스타", "스테이크"},
		{"chinese", "중식", "짜장", "탕수육"},
		{"spicy", "매운", "매콤", "얼큰"},
		{"sweet", "달콤", "단맛"},
		{"savory", "고소", "감칠맛"},
		{"light", "담백", "깔끔"},
		{"rich", "진한", "진하고", "묵직"},
		{"quiet", "조용", "차분"},
		{"lively", "활기", "시끌"},
		{"cozy", "아늑", "편안"},
		{"clean", "청결", "깨끗"},
		{"view", "뷰", "전망"},
		{"date", "데이트", "연인"},
		{"family", "가족", "아이"},
		{"group", "모임", "단체"},
		{"solo", "혼밥", "혼자"},
		{"value", "가성비", "저렴"},
		{"premium", "고급", "프리미엄"},
		{"kind", "친절"},
		{"waiting", "웨이팅", "대기"},
		{"parking", "주차"},
	};

	public static final int DIMENSION = KEYWORDS.length;

	public static int indexOf(String tagCode) {
		for (int i = 0; i < KEYWORDS.length; i++) {
			if (KEYWORDS[i][0].equals(tagCode)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Unknown fake tag code: " + tagCode);
	}

	@Override
	public List<float[]> embed(List<String> texts) {
		List<float[]> vectors = new ArrayList<>();
		for (String text : texts) {
			vectors.add(embedOne(text == null ? "" : text));
		}
		return vectors;
	}

	@Override
	public String version() {
		return VERSION;
	}

	private float[] embedOne(String text) {
		float[] vector = new float[DIMENSION];
		for (int i = 0; i < KEYWORDS.length; i++) {
			for (int k = 1; k < KEYWORDS[i].length; k++) {
				if (text.contains(KEYWORDS[i][k])) {
					vector[i] += 1f;
				}
			}
		}
		return vector;
	}
}
