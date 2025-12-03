package com.example.aicopilot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class ModelCheck {

	// 1. 여기에 회사에서 받은 API Key를 입력하세요.
	private static final String API_KEY = "${openai.api-key}";

	// 2. 테스트할 모델 목록
	private static final List<String> CANDIDATES = List.of(
			"gpt-4o",
			"gpt-4o-mini",
			"gpt-4-turbo",
			"gpt-3.5-turbo",
			"gpt-4"
	);

	public static void main(String[] args) {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();

		System.out.println("🔍 API Key 권한으로 실제 사용 가능한 모델을 검증합니다...\n");

		for (String model : CANDIDATES) {
			checkModel(client, model);
		}
	}

	private static void checkModel(HttpClient client, String model) {
		try {
			// JSON 문자열 생성 (라이브러리 없이 간단히 구성)
			String jsonBody = String.format(
					"{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"hi\"}], \"max_tokens\": 1}",
					model
			);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://api.openai.com/v1/chat/completions"))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + API_KEY)
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			int status = response.statusCode();

			if (status == 200) {
				System.out.println("✅ [사용 가능] " + model);
			} else {
				String reason = parseErrorReason(status);
				System.out.println("❌ [사용 불가] " + model + " (Code: " + status + ") -> " + reason);
				// 필요시 에러 상세 내용 출력
				// System.out.println("   Response: " + response.body());
			}

		} catch (Exception e) {
			System.out.println("❌ [오류 발생] " + model + " -> " + e.getMessage());
		}
	}

	private static String parseErrorReason(int statusCode) {
		switch (statusCode) {
			case 404: return "모델 접근 권한 없음 (Tier 1 미만 또는 모델명 오타)";
			case 429: return "Rate Limit 초과 또는 크레딧(잔액) 부족";
			case 401: return "API Key 인증 실패 (키 값 확인 필요)";
			case 403: return "접근 거부 (WAF, 지역 제한 등)";
			case 500: return "OpenAI 서버 내부 오류";
			default: return "기타 오류";
		}
	}
}