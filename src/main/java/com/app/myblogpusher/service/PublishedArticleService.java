package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service
public class PublishedArticleService {

	public List<PublishedArticleDto> getPublishedArticles(Long repoId) throws IOException {
		String repoPath = System.getProperty("java.io.tmpdir") + "/myblogpusher_" + repoId;
		Path postsDir = Paths.get(repoPath, "content", "posts");

		List<PublishedArticleDto> articles = new ArrayList<>();

		if (!Files.exists(postsDir)) {
			return articles;
		}

		try (Stream<Path> paths = Files.list(postsDir)) {
			paths.filter(p -> p.toString().endsWith(".md"))
				.forEach(p -> {
					try {
						File file = p.toFile();
						String fileName = file.getName().replace(".md", "");
						long lastModified = file.lastModified();
						LocalDateTime updateDate = LocalDateTime.ofInstant(
							java.time.Instant.ofEpochMilli(lastModified),
							ZoneId.systemDefault()
						);

						String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
						String title = extractTitle(content);

						articles.add(new PublishedArticleDto(fileName, title, updateDate, content));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
		}

		return articles.stream()
			.sorted((a, b) -> b.getUpdateDate().compareTo(a.getUpdateDate()))
			.toList();
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

		public String getSlug() { return slug; }
		public String getTitle() { return title; }
		public LocalDateTime getUpdateDate() { return updateDate; }
		public String getContent() { return content; }
	}
}