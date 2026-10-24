/**
 * IPアドレスから接続元の国情報を取得するService
 *
 * IPinfo Lite APIを利用して、
 * ログイン元IPの国コードを取得する。
 *
 * 認証処理そのものは担当せず、
 * IPアドレスから地域情報を取得することだけを担当する。
 */

package com.app.myblogpusher.service.Login;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IpGeolocationService {

	private final ObjectMapper objectMapper;

	private final String token;

	private final HttpClient httpClient;

	public IpGeolocationService(
			ObjectMapper objectMapper,
			@Value("${ipinfo.token}") String token) {

		this.objectMapper = objectMapper;
		this.token = token;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(30))
				.build();
	}

	/**
	 * IPアドレスから国コードを取得する。
	 *
	 * 一時的な通信エラーに備えて最大3回試行する。
	 * 通信に失敗した場合は1秒待機して再試行する。
	 *
	 * @param ipAddress 接続元IPアドレス
	 * @return 国コード。取得できない場合はnull
	 */
	public String findCountryCode(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank()) {
			return null;
		}

		String url = "https://api.ipinfo.io/lite/"
				+ ipAddress
				+ "?token="
				+ token;

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Accept", "application/json")
				.timeout(Duration.ofSeconds(60))
				.GET()
				.build();

		HttpResponse<String> response = null;

		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				response = httpClient.send(
						request,
						HttpResponse.BodyHandlers.ofString());

				break;

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();

				System.err.println(
						"[IpGeolocationTiming] 通信が中断されました");

				return null;

			} catch (IOException e) {
				System.err.println(
						"[IpGeolocationTiming] 通信失敗: 試行="
								+ attempt + "/3, error="
								+ e.getClass().getSimpleName());

				if (attempt == 3) {
					System.err.println(
							"[IpGeolocationTiming] 最大試行回数に達しました");

					return null;
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();

					System.err.println(
							"[IpGeolocationTiming] リトライ待機が中断されました");

					return null;
				}
			}
		}

		if (response == null) {
			return null;
		}

		if (response.statusCode() != 200) {
			System.err.println(
					"[IpGeolocationTiming] HTTPエラー: status="
							+ response.statusCode());

			return null;
		}

		try {
			JsonNode json = objectMapper.readTree(response.body());
			JsonNode countryCode = json.get("country_code");

			if (countryCode == null
					|| countryCode.isNull()
					|| countryCode.asText().isBlank()) {

				System.err.println(
						"[IpGeolocationTiming] country_codeが応答JSONにありません");

				return null;
			}

			String result = countryCode.asText();

			System.out.println(
					"[IpGeolocationTiming] 国コード取得成功: region="
							+ result);

			return result;

		} catch (IOException e) {
			System.err.println(
					"[IpGeolocationTiming] JSON解析に失敗しました: "
							+ e.getMessage());

			return null;
		}
	}
}