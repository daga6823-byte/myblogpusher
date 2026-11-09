/**

* GitHub APIとの通信を担当するサービス
*
* GitHubリポジトリの情報取得、Git Trees APIによるファイル一覧取得、
* Contents APIによるMarkdown本文取得など、GitHub APIへのアクセス処理を担当する。
*
* 取得した記事のDTO変換やDBへの同期処理は担当しない。
  */
package com.app.myblogpusher.service.Github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubApiService {

	/**
	 * GitHub上の記事ファイルの情報
	 *
	 * pathはファイルパス、shaはファイル内容の識別子を保持する。
	 */
	public record GitHubMarkdownFile(
			String path,
			String sha) {
	}

	/**
	 * リポジトリのデフォルトブランチ名を取得する。
	 */
	public String fetchDefaultBranch(
			String owner,
			String repoName,
			String token)
			throws IOException {

		String apiUrl = "https://api.github.com/repos/"
				+ owner + "/"
				+ repoName;

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty(
				"Authorization",
				"token " + token);
		conn.setRequestProperty(
				"Accept",
				"application/vnd.github.v3+json");

		String response = new String(
				conn.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response);

		return json.get("default_branch").asText();
	}

	/**
	 * Git Trees APIでリポジトリ全体のMarkdownファイル情報を取得する。
	 *
	 * カテゴリールート配下の記事について、ファイルパスとSHAを取得する。
	 */
	public List<GitHubMarkdownFile> fetchAllMarkdownFiles(
			String owner,
			String repoName,
			String branch,
			String token)
			throws IOException {

		String apiUrl = "https://api.github.com/repos/"
				+ owner + "/"
				+ repoName
				+ "/git/trees/"
				+ branch
				+ "?recursive=1";

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty(
				"Authorization",
				"token " + token);
		conn.setRequestProperty(
				"Accept",
				"application/vnd.github.v3+json");

		if (conn.getResponseCode() != 200) {
			throw new IOException(
					"GitHubのファイル一覧取得に失敗しました。HTTP status: "
							+ conn.getResponseCode());
		}

		String response = new String(
				conn.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response);
		JsonNode tree = json.get("tree");

		return StreamSupport.stream(
				tree.spliterator(),
				false)
				.filter(node -> "blob".equals(
						node.get("type").asText()))
				.filter(node -> isUnderCategoryRoot(
						node.get("path").asText()))
				.map(node -> new GitHubMarkdownFile(
						node.get("path").asText(),
						node.get("sha").asText()))
				.toList();
	}

	/**
	 * Git Trees APIから記事ファイルのパスだけを取得する。
	 *
	 * 既存の呼び出し元との互換性を維持する。
	 */
	public List<String> fetchAllMarkdownPaths(
			String owner,
			String repoName,
			String branch,
			String token)
			throws IOException {

		return fetchAllMarkdownFiles(
				owner,
				repoName,
				branch,
				token)
						.stream()
						.map(GitHubMarkdownFile::path)
						.toList();
	}

	/**
	 * パスがcontent/{カテゴリールート}/配下の.mdファイルか判定する。
	 */
	private boolean isUnderCategoryRoot(String path) {

		if (!path.startsWith("content/")
				|| !path.endsWith(".md")) {
			return false;
		}

		if (path.endsWith("/_index.md")) {
			return false;
		}

		String rest = path.substring("content/".length());

		return rest.contains("/");
	}

	/**
	 * GitHub Contents APIからMarkdown本文を取得する。
	 */
	public String fetchContentViaApi(
			String url,
			String token)
			throws IOException {

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty(
				"Authorization",
				"token " + token);
		conn.setRequestProperty(
				"Accept",
				"application/vnd.github.v3+json");

		String response = new String(
				conn.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);

		// Base64デコード
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response);

		String encoded = json.get("content")
				.asText()
				.replaceAll("\\s", "");

		return new String(
				java.util.Base64.getDecoder()
						.decode(encoded),
				StandardCharsets.UTF_8);
	}

	/**
	 * GitHub Contents APIから指定したMarkdownファイルを削除する。
	 *
	 * hugoPathはcontent配下からの相対パスを受け取る。
	 * 例: reference/batman/gadget/bat-claw
	 */
	public void deleteMarkdown(
			String owner,
			String repoName,
			String hugoPath,
			String token,
			String sha)
			throws IOException {

		String apiUrl = "https://api.github.com/repos/"
				+ owner + "/"
				+ repoName
				+ "/contents/content/"
				+ hugoPath
				+ ".md";

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();

		conn.setRequestMethod("DELETE");

		conn.setRequestProperty(
				"Authorization",
				"token " + token);
		conn.setRequestProperty(
				"Accept",
				"application/vnd.github.v3+json");
		conn.setRequestProperty(
				"Content-Type",
				"application/json");

		String requestBody = "{"
				+ "\"message\":\"Delete article: "
				+ hugoPath
				+ "\","
				+ "\"sha\":\""
				+ sha
				+ "\""
				+ "}";

		conn.setDoOutput(true);

		conn.getOutputStream().write(
				requestBody.getBytes(StandardCharsets.UTF_8));

		int responseCode = conn.getResponseCode();

		if (responseCode != 200) {
			throw new IOException(
					"GitHub上の記事削除に失敗しました。HTTP status: "
							+ responseCode);
		}
	}
}
