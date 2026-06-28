package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
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

		//		long start = System.currentTimeMillis();

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		System.out.println("Decrypted token length: " + accessToken.length());
		System.out.println("Token starts with: " + accessToken.substring(0, Math.min(10, accessToken.length())));

		String repoPath = System.getProperty("java.io.tmpdir")
				+ "/myblogpusher_"
				+ repoEntity.getRepoId();

		File repoDir = new File(repoPath);

		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}

		ArticleCategory category = articleCategoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("カテゴリーが見つかりません"));

		String categorySlug = SlugUtil.generateCategorySlug(category.getCategoryName());

		Git git = initializeRepository(repoDir, repoEntity, accessToken);

		//		System.out.println("initialize: " + (System.currentTimeMillis() - start) + "ms");

		try {

			createCategoryIndexIfNotExists(
					git,
					repoPath,
					categorySlug,
					category.getCategoryName());

			Path contentPath = Paths.get(
					repoPath,
					"content",
					"posts",
					slug + ".md");

			contentPath.getParent().toFile().mkdirs();

			Files.write(
					contentPath,
					articleContent.getBytes(StandardCharsets.UTF_8));

			//			System.out.println("write: " + (System.currentTimeMillis() - start) + "ms");

			git.add()
					.addFilepattern("content")
					.call();

			//			System.out.println("add: " + (System.currentTimeMillis() - start) + "ms");

			git.commit()
					.setMessage("Add article: " + slug)
					.setAuthor(
							"Myblogpusher",
							"noreply@myblogpusher.local")
					.call();

			//			System.out.println("commit: " + (System.currentTimeMillis() - start) + "ms");

			// Git push
			git.push()
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", accessToken))
					.setRefSpecs(new org.eclipse.jgit.transport.RefSpec("HEAD:refs/heads/main"))
					.call();

			//			System.out.println("push: " + (System.currentTimeMillis() - start) + "ms");

		} finally {
			git.close();
		}
	}

	private void createCategoryIndexIfNotExists(Git git, String repoPath, String categorySlug, String categoryName)
			throws IOException, GitAPIException {

		Path indexPath = Paths.get(repoPath, "content", "categories", categorySlug, "_index.md");

		// 既に存在すればスキップ
		if (Files.exists(indexPath)) {
			return;
		}

		indexPath.getParent().toFile().mkdirs();

		String indexContent = "---\n"
				+ "title: \"" + categoryName + "\"\n"
				+ "description: \"\"\n"
				+ "---\n";

		Files.write(indexPath, indexContent.getBytes(StandardCharsets.UTF_8));

		// Git add
		git.add().addFilepattern("content/categories/" + categorySlug + "/_index.md").call();
	}

	private Git initializeRepository(File repoDir, UserRepositoryEntity repoEntity, String accessToken)
			throws IOException, GitAPIException {

		long start = System.currentTimeMillis();

		File gitDir = new File(repoDir, ".git");

		if (gitDir.exists()) {

			Repository repository = new FileRepositoryBuilder()
					.setGitDir(gitDir)
					.build();

			System.out.println("build: " + (System.currentTimeMillis() - start) + "ms");

			Git git = new Git(repository);

			System.out.println("new Git: " + (System.currentTimeMillis() - start) + "ms");

			git.pull()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(
									"git",
									accessToken))
					.call();

			System.out.println("pull: " + (System.currentTimeMillis() - start) + "ms");

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

		System.out.println("Title: " + title);

		String result = title;

		// 辞書に登録された全ての日本語を検索して置き換え
		List<EnglishDictionary> dictionaries = englishDictionaryRepository.findAll();
		for (EnglishDictionary dict : dictionaries) {
			if (result.contains(dict.getJapanese())) {
				System.out.println("Found: " + dict.getJapanese() + " -> " + dict.getEnglish());
				result = result.replace(dict.getJapanese(), " " + dict.getEnglish() + " ");
			}
		}

		System.out.println("After dictionary replace: " + result);

		String finalSlug = result
				.toLowerCase()
				.replaceAll("[^a-z0-9\\s]", "") // 英数字とスペース以外削除
				.replaceAll("\\s+", "-") // スペースをハイフンに
				.replaceAll("^-+|-+$", "") // 前後のハイフン削除
				.trim();

		System.out.println("Final slug: " + finalSlug);

		return finalSlug;

	}
}
