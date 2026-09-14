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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubApiService {

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
	 * Git Trees APIでリポジトリ全体のファイルパスを再帰的に取得し、
	 * カテゴリールート配下の.mdファイルだけを取得する。
	 */
	public List<String> fetchAllMarkdownPaths(
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
			return new ArrayList<>();
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
				.map(node -> node.get("path").asText())
				.filter(this::isUnderCategoryRoot)
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

}
