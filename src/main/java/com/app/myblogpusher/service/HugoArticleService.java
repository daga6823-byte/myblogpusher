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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.Article.ArticleCategoryRepository;

@Service
public class HugoArticleService {

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	public void createArticle(
			Git git,
			String repoPath,
			Article article,
			String slug)
			throws IOException, GitAPIException {

		String categoryPath = article.getHugoPath();

		createCategoryIndexesRecursively(
		        git,
		        repoPath,
		        categoryPath,
		        article.getUserId());

		// カテゴリー経路とslugを結合して記事ファイルのパスを生成する。
		String hugoPath = article.getHugoPath() + "/" + slug;

		Path contentPath = Paths.get(
		        repoPath,
		        "content",
		        hugoPath + ".md");

		contentPath.getParent().toFile().mkdirs();

		Files.write(
				contentPath,
				article.getContent().getBytes(StandardCharsets.UTF_8));

		git.add()
				.addFilepattern("content")
				.call();
	}

	private void createCategoryIndexesRecursively(
			Git git,
			String repoPath,
			String categoryPath,
			Long userId)
			throws IOException, GitAPIException {

		Path currentDir = Paths.get(repoPath, "content");
		StringBuilder relativePath = new StringBuilder("content");

		for (String categoryName : categoryPath.split("/")) {

			ArticleCategory category = articleCategoryRepository
					.findByUserIdAndCategoryName(userId, categoryName)
					.orElseThrow();

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

	/**
	 * 記事のHugoパスを生成する
	 *
	 * 例：
	 * movie/batman/gadget/grapple-gun
	 */
	public String buildArticlePath(
			Long categoryGroupId,
			String slug) {

		String categoryPath = categoryRelationRepository
				.findByGroupId(categoryGroupId)
				.stream()
				.findFirst()
				.orElseThrow()
				.getCategoryPath();

		return categoryPath + "/" + slug;
	}

	/**
	 * 既存記事のHugoファイルを新しいカテゴリー経路へ移動する。
	 *
	 * 移動元はArticleに保存されたフルパス、
	 * 移動先はカテゴリー経路とslugを結合したフルパスを受け取る。
	 */
	public void moveArticle(
	        Git git,
	        String repoPath,
	        String oldHugoPath,
	        String newHugoPath)
	        throws IOException, GitAPIException {

	    // 移動元と移動先が同じ場合は何もしない。
	    if (oldHugoPath.equals(newHugoPath)) {
	        return;
	    }

	    Path oldPath = Paths.get(
	            repoPath,
	            "content",
	            oldHugoPath + ".md");

	    Path newPath = Paths.get(
	            repoPath,
	            "content",
	            newHugoPath + ".md");

	    if (!Files.exists(oldPath)) {
	        throw new IOException(
	                "移動元の記事ファイルが存在しません: " + oldPath);
	    }

	    // 移動先に別の記事が存在する場合は誤上書きを防止する。
	    if (Files.exists(newPath)) {
	        throw new IOException(
	                "移動先の記事ファイルが既に存在します: " + newPath);
	    }

	    Files.createDirectories(newPath.getParent());

	    Files.move(oldPath, newPath);

	    // Gitインデックスに移動元の削除と移動先の追加を反映する。
	    git.add()
	            .addFilepattern("content/" + oldHugoPath + ".md")
	            .call();

	    git.add()
	            .addFilepattern("content/" + newHugoPath + ".md")
	            .call();
	}
}