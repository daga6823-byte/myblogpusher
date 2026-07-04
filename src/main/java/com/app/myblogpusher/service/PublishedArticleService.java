/**
 * 投稿済み記事の取得を担当するサービス
 * GitHub APIを使用してリポジトリから記事一覧を取得する
 * tmpディレクトリではなくGitHub API経由で取得するためRender環境に対応
 */

package com.app.myblogpusher.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.PublishedArticleDto;
import com.app.myblogpusher.dto.PublishedArticleSummaryDto;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.util.FrontMatterUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

@Service
public class PublishedArticleService {

	@Autowired
	private TokenCipherService tokenCipherService;

	@Autowired
	private FrontMatterUtil frontMatterUtil;

	
	public List<PublishedArticleSummaryDto> getPublishedArticles(UserRepositoryEntity repo, String cipherKey, HttpSession session)
			throws IOException {

		// キャッシュを確認
		@SuppressWarnings("unchecked")
		List<PublishedArticleSummaryDto> cached = (List<PublishedArticleSummaryDto>) session.getAttribute("publishedArticlesCache");
		if (cached != null) {
			return cached;
		}

		String accessToken = tokenCipherService.decrypt(
				repo.getAccessToken(),
				repo.getTokenIv(),
				cipherKey);

		String apiUrl = "https://api.github.com/repos/"
				+ repo.getRepoOwner() + "/" + repo.getRepoName()
				+ "/contents/content/posts";

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty("Authorization", "token " + accessToken);
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

		if (conn.getResponseCode() != 200) {
			return new ArrayList<>();
		}

		String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode files = mapper.readTree(response);

		List<PublishedArticleSummaryDto> result = StreamSupport.stream(files.spliterator(), true)
			.filter(file -> file.get("name").asText().endsWith(".md"))
			.map(file -> {
				try {
					String fileName = file.get("name").asText();
					String fileApiUrl = file.get("url").asText();
					String mdContent = fetchContentViaApi(fileApiUrl, accessToken);
					String slug = fileName.replace(".md", "");
					String title = frontMatterUtil.extractTitle(mdContent);
					return new PublishedArticleSummaryDto(slug, title, LocalDateTime.now());
				} catch (IOException e) {
					return null;
				}
			})
			.filter(a -> a != null)
			.sorted((a, b) -> b.getUpdateDate().compareTo(a.getUpdateDate()))
			.toList();

		System.out.println("Articles count: " + result.size());

		session.setAttribute("publishedArticlesCache", result);
		return result;
	}

	public PublishedArticleDto getPublishedArticle(UserRepositoryEntity repo, String cipherKey, String slug) throws IOException {
		String accessToken = tokenCipherService.decrypt(
				repo.getAccessToken(),
				repo.getTokenIv(),
				cipherKey);

		String apiUrl = "https://api.github.com/repos/"
				+ repo.getRepoOwner() + "/" + repo.getRepoName()
				+ "/contents/content/posts/" + slug + ".md";

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty("Authorization", "token " + accessToken);
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

		if (conn.getResponseCode() != 200) {
			return null;
		}

		String mdContent = fetchContentViaApi(apiUrl, accessToken);
		String title = frontMatterUtil.extractTitle(mdContent);
		List<String> categories = frontMatterUtil.extractCategories(mdContent);

		return new PublishedArticleDto(slug, title, LocalDateTime.now(), mdContent, categories);
	}	

	private String fetchContentViaApi(String url, String token) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("Authorization", "token " + token);
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

		String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		// Base64デコード
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response);
		String encoded = json.get("content").asText().replaceAll("\\s", "");
		return new String(java.util.Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
	}
	
}