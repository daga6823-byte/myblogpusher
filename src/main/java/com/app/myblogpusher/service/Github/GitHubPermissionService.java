/**
 * GitHub APIを使ってリポジトリへの投稿権限を確認するサービス
 *
 * Gitの作業ディレクトリには触れず、REST APIの結果だけで判定する。
 */

package com.app.myblogpusher.service.Github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.service.TokenCipherService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubPermissionService {

	private final TokenCipherService tokenCipherService;

	public GitHubPermissionService(
			TokenCipherService tokenCipherService) {

		this.tokenCipherService = tokenCipherService;
	}

	/**
	 * GitHub APIでリポジトリへの投稿権限を確認する
	 */
	public boolean canPublish(
			UserRepositoryEntity repoEntity,
			String cipherKey)
			throws Exception {

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		String apiUrl = String.format(
				"https://api.github.com/repos/%s/%s",
				repoEntity.getRepoOwner(),
				repoEntity.getRepoName());

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.header(
						"Authorization",
						"Bearer " + accessToken)
				.header(
						"Accept",
						"application/vnd.github+json")
				.header(
						"X-GitHub-Api-Version",
						"2022-11-28")
				.GET()
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(
						request,
						HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			return false;
		}

		JsonNode permissions = new ObjectMapper()
				.readTree(response.body())
				.get("permissions");

		return permissions != null
				&& permissions.path("push").asBoolean(false);
	}
}