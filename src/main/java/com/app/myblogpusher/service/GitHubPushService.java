/**
 * 記事をGitHubリポジトリにMarkdownファイルとしてプッシュするサービス
 * カテゴリーの親子階層（parentCategoryId）を辿ってcontent配下のパスを組み立て、
 * パス上の各階層に日本語表示名(displayName)入りの_index.mdを自動生成する
 */

package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.EnglishDictionary;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.ArticleCategoryRepository;
import com.app.myblogpusher.repository.EnglishDictionaryRepository;
import com.app.myblogpusher.util.SlugUtil;

@Service
public class GitHubPushService {

	private final TokenCipherService tokenCipherService;
	private final ArticleCategoryRepository articleCategoryRepository;
	private final ArticleWorkService articleWorkService;
	private final EnglishDictionaryRepository englishDictionaryRepository;

	@Autowired
	private SlugUtil slugUtil;

	public GitHubPushService(
			TokenCipherService tokenCipherService,
			ArticleCategoryRepository articleCategoryRepository,
			ArticleWorkService articleWorkService,
			EnglishDictionaryRepository englishDictionaryRepository) {

		this.tokenCipherService = tokenCipherService;
		this.articleCategoryRepository = articleCategoryRepository;
		this.articleWorkService = articleWorkService;
		this.englishDictionaryRepository = englishDictionaryRepository;
	}

	/**
	 * 記事をMarkdownファイルとしてGitHubにプッシュ
	 */
	public void pushArticle(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			Long categoryId,
			String articleTitle,
			String articleContent,
			String slug)
			throws IOException, GitAPIException {

		System.out.println("pushArticle start");

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		String repoPath = System.getProperty("java.io.tmpdir")
				+ "/myblogpusher_"
				+ repoEntity.getRepoId();

		File repoDir = new File(repoPath);

		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}

		ArticleCategory category = articleCategoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("カテゴリーが見つかりません"));

		// カテゴリーの親を辿り、ルートから自分自身までのエンティティリストを作る（例: [movie, batman]）
		List<ArticleCategory> categoryPath = buildCategoryPath(category);
		String categoryPathStr = categoryPath.stream()
				.map(c -> slugUtil.generateCategorySlug(c.getCategoryName()))
				.reduce((a, b) -> a + "/" + b)
				.orElse("");

		Git git = initializeRepository(repoDir, repoEntity, accessToken);

		try {
			// パス上の各階層に _index.md が無ければ作成する（titleは日本語表示名）
			createCategoryIndexesRecursively(git, repoPath, categoryPath);

			Path contentPath = Paths.get(
					repoPath,
					"content",
					categoryPathStr,
					slug + ".md");

			contentPath.getParent().toFile().mkdirs();

			Files.write(
					contentPath,
					articleContent.getBytes(StandardCharsets.UTF_8));

			git.add()
					.addFilepattern("content")
					.call();

			git.commit()
					.setMessage("Add article: " + slug)
					.setAuthor(
							"Myblogpusher",
							"noreply@myblogpusher.local")
					.call();

			git.push()
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", accessToken))
					.setRefSpecs(new org.eclipse.jgit.transport.RefSpec("HEAD:refs/heads/main"))
					.call();

		} finally {
			git.close();
		}
	}

	/**
	 * カテゴリーから親を辿り、ルート(movie等)から自分自身までのエンティティリストを返す
	 * 例: batman_review(親=movie) -> [movieエンティティ, batman_reviewエンティティ]
	 */
	private List<ArticleCategory> buildCategoryPath(ArticleCategory category) {
		List<ArticleCategory> path = new ArrayList<>();
		ArticleCategory current = category;

		while (current != null) {
			path.add(0, current);
			Long parentId = current.getParentCategoryId();
			current = (parentId != null)
					? articleCategoryRepository.findById(parentId).orElse(null)
					: null;
		}

		return path;
	}

	/**
	 * content/{slug1}/_index.md, content/{slug1}/{slug2}/_index.md ...
	 * のように、パス上の各階層に _index.md が無ければ作成する
	 * フォルダ名はカテゴリー名から生成したスラッグ、titleは日本語のdisplayNameを使用
	 */
	private void createCategoryIndexesRecursively(Git git, String repoPath, List<ArticleCategory> categoryPath)
			throws IOException, GitAPIException {

		Path currentDir = Paths.get(repoPath, "content");
		StringBuilder relativePath = new StringBuilder("content");

		for (ArticleCategory category : categoryPath) {
			String slug = slugUtil.generateCategorySlug(category.getCategoryName());
			String title = (category.getDisplayName() != null && !category.getDisplayName().isBlank())
					? category.getDisplayName()
					: category.getCategoryName();

			currentDir = currentDir.resolve(slug);
			relativePath.append("/").append(slug);

			Path indexPath = currentDir.resolve("_index.md");

			if (!Files.exists(indexPath)) {
				currentDir.toFile().mkdirs();

				String indexContent = "---\n"
						+ "title: \"" + title + "\"\n"
						+ "description: \"\"\n"
						+ "---\n";

				Files.write(indexPath, indexContent.getBytes(StandardCharsets.UTF_8));

				git.add().addFilepattern(relativePath + "/_index.md").call();
			}
		}
	}

	private Git initializeRepository(File repoDir, UserRepositoryEntity repoEntity, String accessToken)
			throws IOException, GitAPIException {

		File gitDir = new File(repoDir, ".git");

		if (gitDir.exists()) {

			Repository repository = new FileRepositoryBuilder()
					.setGitDir(gitDir)
					.build();

			Git git = new Git(repository);

			git.pull()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(
									"git",
									accessToken))
					.call();

			return git;

		} else {
			String remoteUrl = String.format("https://github.com/%s/%s.git",
					repoEntity.getRepoOwner(),
					repoEntity.getRepoName());

			return Git.cloneRepository()
					.setURI(remoteUrl)
					.setDirectory(repoDir)
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", accessToken))
					.call();
		}
	}

	@Async
	public void pushArticleAsync(UserRepositoryEntity repoEntity, String cipherKey,
			Long categoryId, String articleTitle, String articleContent,
			String slug, Long workId, Long userId) {
		try {
			// 本文内の画像URLをSupabase Storage URLに変換
			String supabaseBaseUrl = "https://fiobrqfdsebtpbgnhdpz.supabase.co/storage/v1/object/public/blog-images";
			String convertedContent = articleContent.replaceAll(
					"!\\[([^]]*)\\]\\(/images/([^/]+)/([^)]+)\\)",
					"![](" + supabaseBaseUrl + "/$2/$3)");

			// 変換後のcontentでプッシュ
			pushArticle(repoEntity, cipherKey, categoryId, articleTitle, convertedContent, slug);

			// GitHub プッシュ成功後、article_workに保存
			if (workId != null) {
				this.articleWorkService.updateArticleWork(workId, categoryId, articleTitle, convertedContent, userId,
						slug);
			} else {
				this.articleWorkService.insertArticleWork(userId, categoryId, articleTitle, convertedContent, slug);
			}
		} catch (Exception e) {
			System.err.println("投稿処理に失敗しました: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public String generateSlugWithDictionary(String title) {
		if (title == null || title.isBlank()) {
			return "no-title";
		}

		String result = title;

		// 辞書に登録された全ての日本語を検索して置き換え
		List<EnglishDictionary> dictionaries = englishDictionaryRepository.findAll();
		for (EnglishDictionary dict : dictionaries) {
			if (result.contains(dict.getJapanese())) {
				result = result.replace(dict.getJapanese(), " " + dict.getEnglish() + " ");
			}
		}

		String finalSlug = result
				.toLowerCase()
				.replaceAll("[^a-z0-9\\s]", "") // 英数字とスペース以外削除
				.replaceAll("\\s+", "-") // スペースをハイフンに
				.replaceAll("^-+|-+$", "") // 前後のハイフン削除
				.trim();

		return finalSlug;

	}
}