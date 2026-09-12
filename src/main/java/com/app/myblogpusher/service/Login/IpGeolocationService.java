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
		this.httpClient = HttpClient.newHttpClient();
	}

	/**
	 * IPアドレスから国コードを取得する。
	 *
	 * @param ipAddress 接続元IPアドレス
	 * @return ISO 3166-1 alpha-2形式の国コード
	 */
	public String findCountryCode(String ipAddress) {

		if (ipAddress == null || ipAddress.isBlank()) {
			return null;
		}

		try {
			String url = "https://api.ipinfo.io/lite/"
					+ ipAddress
					+ "?token="
					+ token;

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Accept", "application/json")
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(
					request,
					HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				return null;
			}

			JsonNode json = objectMapper.readTree(response.body());

			JsonNode countryCode = json.get("country_code");

			if (countryCode == null || countryCode.isNull()) {
				return null;
			}

			return countryCode.asText();

		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
}