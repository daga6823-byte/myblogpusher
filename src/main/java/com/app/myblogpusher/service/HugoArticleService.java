/**
 * Hugoの記事ファイル生成を担当するサービス
 *
 * カテゴリー階層からパスを生成し、
 * _index.md と記事Markdownを作成する。
 */

package com.app.myblogpusher.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.repository.ArticleCategoryRepository;

@Service
public class HugoArticleService {

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	public void createArticle(
			Git git,
			String repoPath,
			Article article)
			throws IOException, GitAPIException {

		ArticleCategory category = articleCategoryRepository
				.findById(article.getCategoryId())
				.orElseThrow();

		List<ArticleCategory> categoryPath = buildCategoryPath(category);

		createCategoryIndexesRecursively(git, repoPath, categoryPath);

		String categoryPathStr = categoryPath.stream()
				.map(ArticleCategory::getCategoryName)
				.reduce((a, b) -> a + "/" + b)
				.orElse("");

		Path contentPath = Paths.get(
				repoPath,
				"content",
				categoryPathStr,
				article.getSlug() + ".md");

		contentPath.getParent().toFile().mkdirs();

		Files.write(
				contentPath,
				article.getContent().getBytes(StandardCharsets.UTF_8));

		git.add()
				.addFilepattern("content")
				.call();
	}

	private List<ArticleCategory> buildCategoryPath(ArticleCategory category) {

		List<ArticleCategory> path = new ArrayList<>();

		ArticleCategory current = category;

		while (current != null) {

			path.add(0, current);

			current = current.getParentCategoryId() == null
					? null
					: articleCategoryRepository.findById(current.getParentCategoryId()).orElse(null);
		}

		return path;
	}

	private void createCategoryIndexesRecursively(
			Git git,
			String repoPath,
			List<ArticleCategory> categoryPath)
			throws IOException, GitAPIException {

		Path currentDir = Paths.get(repoPath, "content");
		StringBuilder relativePath = new StringBuilder("content");

		for (ArticleCategory category : categoryPath) {

			String slug = category.getCategoryName();

			String title = category.getDisplayName() == null
					? category.getCategoryName()
					: category.getDisplayName();

			currentDir = currentDir.resolve(slug);

			relativePath.append("/").append(slug);

			Path index = currentDir.resolve("_index.md");

			if (!Files.exists(index)) {

				currentDir.toFile().mkdirs();

				String text = "---\n" +
						"title: \"" + title + "\"\n" +
						"description: \"\"\n" +
						"---\n";

				Files.write(index, text.getBytes(StandardCharsets.UTF_8));

				git.add()
						.addFilepattern(relativePath + "/_index.md")
						.call();
			}
		}
	}
}