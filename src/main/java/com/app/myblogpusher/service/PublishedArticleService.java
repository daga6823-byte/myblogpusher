package com.app.myblogpusher.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.util.FrontMatterUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PublishedArticleService {

	@Autowired
	private TokenCipherService tokenCipherService;

	@Autowired
	private FrontMatterUtil frontMatterUtil;

	public List<PublishedArticleDto> getPublishedArticles(UserRepositoryEntity repo, String cipherKey)
			throws IOException {

		String accessToken = tokenCipherService.decrypt(
				repo.getAccessToken(),
				repo.getTokenIv(),
				cipherKey);

		System.out.println("Token length: " + accessToken.length());
		System.out.println("Token prefix: " + accessToken.substring(0, Math.min(10, accessToken.length())));

		String apiUrl = "https://api.github.com/repos/"
				+ repo.getRepoOwner() + "/" + repo.getRepoName()
				+ "/contents/content/posts";

		System.out.println("GitHub API URL: " + apiUrl);

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty("Authorization", "token " + accessToken);
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

		System.out.println("Response code: " + conn.getResponseCode());

		if (conn.getResponseCode() != 200) {
			return new ArrayList<>();
		}

		// ファイル一覧をパース
		String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		List<PublishedArticleDto> articles = new ArrayList<>();

		// JSONパース（org.json or Jackson）
		ObjectMapper mapper = new ObjectMapper();
		JsonNode files = mapper.readTree(response);

		for (JsonNode file : files) {
			String fileName = file.get("name").asText();
			if (!fileName.endsWith(".md"))
				continue;

			String fileApiUrl = file.get("url").asText();
			String mdContent = fetchContentViaApi(fileApiUrl, accessToken);
			String slug = fileName.replace(".md", "");
			String title = frontMatterUtil.extractTitle(mdContent);
			List<String> categories = frontMatterUtil.extractCategories(mdContent);  // ← 追加

			articles.add(new PublishedArticleDto(slug, title, LocalDateTime.now(), mdContent, categories));		
		}
		return articles.stream()
				.sorted((a, b) -> b.getUpdateDate().compareTo(a.getUpdateDate()))
				.toList();
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

	public PublishedArticleDto getPublishedArticle(UserRepositoryEntity repo, String cipherKey, String slug)
			throws IOException {
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

	public static class PublishedArticleDto {
		private String slug;
		private String title;
		private LocalDateTime updateDate;
		private String content;
		private List<String> categories;

		public PublishedArticleDto(String slug, String title, LocalDateTime updateDate, String content,
				List<String> categories) {

			this.slug = slug;
			this.title = title;
			this.updateDate = updateDate;
			this.content = content;
			this.categories = categories;
		}

		public String getSlug() {
			return slug;
		}

		public String getTitle() {
			return title;
		}

		public LocalDateTime getUpdateDate() {
			return updateDate;
		}

		public String getContent() {
			return content;
		}

		public List<String> getCategories() {
			return categories;
		}
	}
}