package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;

@Service
public class GitHubPushService {

	private final TokenCipherService tokenCipherService;

	public GitHubPushService(TokenCipherService tokenCipherService) {
		this.tokenCipherService = tokenCipherService;
	}

	/**
	 * 記事をMarkdownファイルとしてGitHubにプッシュ
	 */
	public void pushArticle(UserRepositoryEntity repoEntity, String cipherKey,
			String articleTitle, String articleContent) throws IOException, GitAPIException {

		// トークン復号
		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		// ローカルリポジトリ初期化（キャッシュディレクトリ）
		String repoPath = System.getProperty("java.io.tmpdir") + "/myblogpusher_" + repoEntity.getRepoId();
		File repoDir = new File(repoPath);

		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}

		Git git = initializeRepository(repoDir, repoEntity, accessToken);

		try {
			// 記事ファイルを書き込み（content/postsフォルダを想定）
			String fileName = generateFileName(articleTitle);
			Path contentPath = Paths.get(repoPath, "content", "posts", fileName);
			contentPath.getParent().toFile().mkdirs();

			Files.write(contentPath, articleContent.getBytes(StandardCharsets.UTF_8));

			// Git add
			git.add().addFilepattern("content/posts/" + fileName).call();

			// Git commit
			String commitMessage = "Add article: " + articleTitle;
			git.commit()
					.setMessage(commitMessage)
					.setAuthor("Myblogpusher", "noreply@myblogpusher.local")
					.call();

			// Git push
			git.push()
					.setCredentialsProvider(new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
							"git", accessToken))
					.call();

		} finally {
			git.close();
		}
	}

	private Git initializeRepository(File repoDir, UserRepositoryEntity repoEntity, String accessToken)
			throws IOException, GitAPIException {

		File gitDir = new File(repoDir, ".git");

		if (gitDir.exists()) {
			// 既存リポジトリを開く
			Repository repository = new FileRepositoryBuilder()
					.setGitDir(gitDir)
					.build();
			return new Git(repository);
		} else {
			// リモートリポジトリをクローン
			String remoteUrl = String.format("https://github.com/%s/%s.git",
					repoEntity.getRepoOwner(),
					repoEntity.getRepoName());

			return Git.cloneRepository()
					.setURI(remoteUrl)
					.setDirectory(repoDir)
					.setCredentialsProvider(new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
							"git", accessToken))
					.call();
		}
	}

	private String generateFileName(String articleTitle) {
		// タイトルをスラッグ化（例：「My Article」→「my-article.md」）
		String slug = articleTitle.toLowerCase()
				.replaceAll("[^a-z0-9\\s]", "")
				.replaceAll("\\s+", "-");

		return slug + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".md";
	}
}