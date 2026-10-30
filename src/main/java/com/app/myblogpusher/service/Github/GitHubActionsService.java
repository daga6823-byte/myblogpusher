
/**
 * GitHub Actionsの実行状態取得を担当するサービス
 *
 * 指定されたコミットSHAに紐づくWorkflow実行一覧を
 * GitHub REST APIから取得する。
 */
package com.app.myblogpusher.service.Github;

import java.io.IOException;
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
public class GitHubActionsService {

	private final TokenCipherService tokenCipherService;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public GitHubActionsService(
			TokenCipherService tokenCipherService,
			ObjectMapper objectMapper) {

		this.tokenCipherService = tokenCipherService;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newHttpClient();
	}

	/**
	 * 指定コミットに紐づくGitHub Actionsの実行一覧を取得する
	 */
	public JsonNode getWorkflowRuns(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String commitSha) throws Exception {

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		String apiUrl = String.format(
				"https://api.github.com/repos/%s/%s/actions/runs?head_sha=%s&per_page=100",
				repoEntity.getRepoOwner(),
				repoEntity.getRepoName(),
				commitSha);

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

		HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException(
					"GitHub Actions取得失敗: HTTP "
							+ response.statusCode());
		}

		JsonNode workflowRuns = objectMapper.readTree(response.body())
				.path("workflow_runs");

		for (JsonNode workflowRun : workflowRuns) {
			if ("Deploy Hugo site to Pages".equals(
					workflowRun.path("name").asText())
					&& "main".equals(
							workflowRun.path("head_branch").asText())
					&& commitSha.equals(
							workflowRun.path("head_sha").asText())
					&& "push".equals(
							workflowRun.path("event").asText())) {
				return workflowRun;
			}
		}

		return null;
	}

	/**
	 * GitHub Pagesへのデプロイ完了を待機する
	 *
	 * 対象コミットのWorkflowを定期的に確認し、
	 * デプロイ成功時のみ正常終了する。
	 */
	public void waitForDeployment(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String commitSha) throws Exception {

		long startTime = System.currentTimeMillis();
		long timeoutMillis = 15 * 60 * 1000;

		while (System.currentTimeMillis() - startTime < timeoutMillis) {

			JsonNode workflowRun = getWorkflowRuns(
					repoEntity,
					cipherKey,
					commitSha);

			if (workflowRun != null
					&& "completed".equals(
							workflowRun.path("status").asText())) {

				String conclusion = workflowRun
						.path("conclusion")
						.asText();

				if ("success".equals(conclusion)) {
					return;
				}

				throw new IOException(
						"GitHub Pagesのデプロイに失敗しました: "
								+ conclusion);
			}

			try {
				Thread.sleep(10_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(
						"GitHub Actionsの完了待機が中断されました。",
						e);
			}
		}

		throw new IOException(
				"GitHub Actionsのデプロイがタイムアウトしました。");
	}
}
