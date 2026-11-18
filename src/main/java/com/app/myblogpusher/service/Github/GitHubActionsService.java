
/**
 * GitHub Actionsの実行状態取得を担当するサービス
 *
 * 指定されたコミットSHAに紐づくWorkflow実行一覧を
 * GitHub REST APIから取得する。
 */
package com.app.myblogpusher.service.Github;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

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

				// 失敗したJob・Stepと実行ログから原因を取得する。
				String failureDetail;

				try {
					failureDetail = getFailureDetail(
							repoEntity,
							accessToken,
							workflowRun);

				} catch (Exception e) {
					failureDetail = "失敗原因の詳細取得に失敗しました: "
							+ e.getMessage();
				}

				String errorMessage = "GitHub Pagesのデプロイに失敗しました: "
						+ conclusion;

				if (failureDetail == null || failureDetail.isBlank()) {
					throw new IOException(errorMessage);
				}

				// 外側の例外メッセージは短く保つ。
				// 原因例外には具体的な失敗内容を格納する。
				// ApplicationErrorLogServiceは原因例外まで辿って記録する。
				throw new IOException(
						errorMessage,
						new IOException(failureDetail));
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

	/**
	 * Workflowの失敗したJob・Stepと実行ログから失敗原因を取得する。
	 *
	 * @return 失敗したJob・Stepとログから抽出したエラー情報
	 */
	private String getFailureDetail(
			UserRepositoryEntity repoEntity,
			String accessToken,
			JsonNode workflowRun) throws Exception {

		String runId = workflowRun.path("id").asText();

		String apiUrl = String.format(
				"https://api.github.com/repos/%s/%s/actions/runs/%s/jobs?per_page=100",
				repoEntity.getRepoOwner(),
				repoEntity.getRepoName(),
				runId);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.GET()
				.build();

		HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException(
					"Workflow Job取得失敗: HTTP "
							+ response.statusCode());
		}

		JsonNode jobs = objectMapper.readTree(response.body())
				.path("jobs");

		StringBuilder detail = new StringBuilder();

		for (JsonNode job : jobs) {

			String jobConclusion = job.path("conclusion").asText();

			// 成功・スキップされたJobは対象外。
			if ("success".equals(jobConclusion)
					|| "skipped".equals(jobConclusion)
					|| jobConclusion.isBlank()) {
				continue;
			}

			String jobName = job.path("name").asText();
			String jobId = job.path("id").asText();

			detail.append("Job: ")
					.append(jobName)
					.append(" / ")
					.append(jobConclusion)
					.append('\n');

			// 失敗したStepを特定する。
			for (JsonNode step : job.path("steps")) {

				String stepConclusion = step.path("conclusion").asText();

				if ("failure".equals(stepConclusion)
						|| "timed_out".equals(stepConclusion)
						|| "cancelled".equals(stepConclusion)) {

					detail.append("Step: ")
							.append(step.path("name").asText())
							.append(" / ")
							.append(stepConclusion)
							.append('\n');
				}
			}

			// Jobの実行ログからエラー情報を抽出する。
			try {
				String jobLog = downloadJobLog(
						repoEntity,
						accessToken,
						jobId);

				String errorLines = extractErrorLines(jobLog);

				if (!errorLines.isBlank()) {
					detail.append(errorLines);
				}

			} catch (Exception e) {
				detail.append("Jobログ取得失敗: ")
						.append(e.getMessage())
						.append('\n');
			}
		}

		return detail.toString();
	}

	/**
	 * GitHub ActionsのJob実行ログを取得する。
	 *
	 * GitHub APIから返される一時ダウンロードURLを使用する。
	 * ログの読み込み量は最大1MBに制限する。
	 */
	private String downloadJobLog(
			UserRepositoryEntity repoEntity,
			String accessToken,
			String jobId) throws Exception {

		String apiUrl = String.format(
				"https://api.github.com/repos/%s/%s/actions/jobs/%s/logs",
				repoEntity.getRepoOwner(),
				repoEntity.getRepoName(),
				jobId);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", "application/vnd.github+json")
				.header("X-GitHub-Api-Version", "2022-11-28")
				.GET()
				.build();

		// GitHub APIから一時ダウンロードURLを取得する。
		HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 302) {
			throw new IOException(
					"JobログURL取得失敗: HTTP "
							+ response.statusCode());
		}

		String downloadUrl = response.headers()
				.firstValue("Location")
				.orElseThrow(() -> new IOException(
						"JobログのダウンロードURLがありません。"));

		// 一時URLにはGitHubのアクセストークンを付けない。
		HttpRequest downloadRequest = HttpRequest.newBuilder()
				.uri(URI.create(downloadUrl))
				.GET()
				.build();

		HttpResponse<InputStream> downloadResponse = httpClient.send(
				downloadRequest,
				HttpResponse.BodyHandlers.ofInputStream());

		if (downloadResponse.statusCode() != 200) {
			downloadResponse.body().close();

			throw new IOException(
					"Jobログダウンロード失敗: HTTP "
							+ downloadResponse.statusCode());
		}

		try (InputStream inputStream = downloadResponse.body()) {

			byte[] logBytes = inputStream.readNBytes(1024 * 1024);

			return new String(
					logBytes,
					StandardCharsets.UTF_8);
		}
	}

	/**
	 * Jobログからエラーに関係する行を抽出する。
	 *
	 * ログ全文は保存せず、エラー調査に必要な行だけを取得する。
	 */
	private String extractErrorLines(String jobLog) {

		if (jobLog == null || jobLog.isBlank()) {
			return "";
		}

		StringBuilder result = new StringBuilder();

		String[] lines = jobLog.split("\\R");

		int extractedCount = 0;
		int maxLines = 10;
		int maxLength = 3000;

		for (String line : lines) {

			String lowerLine = line.toLowerCase();

			if (lowerLine.contains("error")
					|| lowerLine.contains("failed")
					|| lowerLine.contains("fatal")
					|| lowerLine.contains("exception")
					|| lowerLine.contains("sass")) {

				// 1行が長すぎる場合も保存量を制限する。
				String extractedLine = line.length() > 1000
						? line.substring(0, 1000)
						: line;

				if (result.length() + extractedLine.length() > maxLength) {
					break;
				}

				result.append(extractedLine).append('\n');

				extractedCount++;

				if (extractedCount >= maxLines) {
					break;
				}
			}
		}

		return result.toString();
	}
}
