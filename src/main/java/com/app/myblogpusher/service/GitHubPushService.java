package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.ArticleCategoryRepository;

@Service
public class GitHubPushService {

	private final TokenCipherService tokenCipherService;

	private final ArticleCategoryRepository articleCategoryRepository;

	public GitHubPushService(
			TokenCipherService tokenCipherService,
			ArticleCategoryRepository articleCategoryRepository) {

		this.tokenCipherService = tokenCipherService;
		this.articleCategoryRepository = articleCategoryRepository;
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

		long start = System.currentTimeMillis();
		
		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);
//
//		System.out.println("Decrypted token length: " + accessToken.length());
//		System.out.println("Token starts with: " + accessToken.substring(0, Math.min(10, accessToken.length())));

		String repoPath = System.getProperty("java.io.tmpdir")
				+ "/myblogpusher_"
				+ repoEntity.getRepoId();

		File repoDir = new File(repoPath);

		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}

		ArticleCategory category = articleCategoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("カテゴリーが見つかりません"));

		String categorySlug = generateCategorySlug(category.getCategoryName());

		Git git = initializeRepository(repoDir, repoEntity, accessToken);

		System.out.println("initialize: " + (System.currentTimeMillis() - start) + "ms");
		
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

			System.out.println("write: " + (System.currentTimeMillis() - start) + "ms");
			
			git.add()
					.addFilepattern("content")
					.call();

			System.out.println("add: " + (System.currentTimeMillis() - start) + "ms");
			
			git.commit()
					.setMessage("Add article: " + slug)
					.setAuthor(
							"Myblogpusher",
							"noreply@myblogpusher.local")
					.call();

			System.out.println("commit: " + (System.currentTimeMillis() - start) + "ms");
			
			// Git push
			git.push()
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", accessToken))
					.setRefSpecs(new org.eclipse.jgit.transport.RefSpec("HEAD:refs/heads/main"))
					.call();
			
			System.out.println("push: " + (System.currentTimeMillis() - start) + "ms");

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

		File gitDir = new File(repoDir, ".git");

		if (gitDir.exists()) {

			Repository repository = new FileRepositoryBuilder()
					.setGitDir(gitDir)
					.readEnvironment()
					.findGitDir()
					.build();

			Git git = new Git(repository);

			git.pull()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(
									"git",
									accessToken))
					.call();

			return git;

		} else

		{
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

	private String generateCategorySlug(String categoryName) {
		// カテゴリー名をスラッグ化（例：「バンビ～ノ！レビュー」→「bambino-review」）
		// 簡易版：スペースをハイフンに、記号を削除
		return categoryName.toLowerCase()
				.replaceAll("[^a-z0-9\\s]", "")
				.replaceAll("\\s+", "_");
	}
}