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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PublishedArticleService {

	@Autowired
    private TokenCipherService tokenCipherService;
	
	public List<PublishedArticleDto> getPublishedArticles(UserRepositoryEntity repo, String cipherKey)
			throws IOException {

		String accessToken = tokenCipherService.decrypt(
				repo.getAccessToken(),
				repo.getTokenIv(),
				cipherKey);

		String apiUrl = "https://api.github.com/repos/"
				+ repo.getRepoOwner() + "/" + repo.getRepoName()
				+ "/contents/content/posts";
		
		System.out.println("GitHub API URL: " + apiUrl);

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty("Authorization", "Bearer " + repo.getAccessToken());
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

			String downloadUrl = file.get("download_url").asText();
			String mdContent = fetchRawContent(downloadUrl, repo.getAccessToken());
			String slug = fileName.replace(".md", "");
			String title = extractTitle(mdContent);

			articles.add(new PublishedArticleDto(slug, title, LocalDateTime.now(), mdContent));
		}

		return articles.stream()
				.sorted((a, b) -> b.getUpdateDate().compareTo(a.getUpdateDate()))
				.toList();
	}

	private String fetchRawContent(String url, String token) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("Authorization", "Bearer " + token);
		return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}

	private String extractTitle(String content) {
		// Front matter から title を抽出
		if (content.startsWith("---")) {
			String[] parts = content.split("---", 3);
			if (parts.length >= 2) {
				String frontMatter = parts[1];
				for (String line : frontMatter.split("\n")) {
					if (line.startsWith("title:")) {
						return line.replace("title:", "").replace("\"", "").trim();
					}
				}
			}
		}
		return "（タイトルなし）";
	}

	public static class PublishedArticleDto {
		private String slug;
		private String title;
		private LocalDateTime updateDate;
		private String content;

		public PublishedArticleDto(String slug, String title, LocalDateTime updateDate, String content) {
			this.slug = slug;
			this.title = title;
			this.updateDate = updateDate;
			this.content = content;
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
	}
}