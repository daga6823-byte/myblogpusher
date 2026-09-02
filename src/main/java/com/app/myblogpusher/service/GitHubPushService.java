/**
 * 記事をGitHubリポジトリにMarkdownファイルとしてプッシュするサービス
 *
 * Git操作、認証、非同期投稿処理を担当する。
 * Hugo用Markdown生成はHugoArticleServiceへ委譲する。
 */

package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.util.ArticleImageUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubPushService {

	private final TokenCipherService tokenCipherService;

	@Autowired
	private HugoArticleService hugoArticleService;

	@Autowired
	private ArticleImageUtil articleImageUtil;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private ArticleWorkService articleWorkService;

	public GitHubPushService(
			TokenCipherService tokenCipherService) {

		this.tokenCipherService = tokenCipherService;
	}

	/**
	 * 記事をMarkdownファイルとしてGitHubへプッシュ
	 */
	public void pushArticle(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			Article article,
			boolean newArticle,
			String slug)
			throws IOException, GitAPIException {

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

		Git git = initializeRepository(
				repoDir,
				repoEntity,
				accessToken);

		try {

			hugoArticleService.createArticle(
					git,
					repoPath,
					article,
					slug);

			String commitMessage = newArticle
					? "Add article: "
					: "Update article: ";

			git.commit()
					.setMessage(commitMessage + article.getSlug())
					.setAuthor(
							"Myblogpusher",
							"noreply@myblogpusher.local")
					.call();

			git.push()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(
									"git",
									accessToken))
					.call();

		} finally {
			git.close();
		}
	}

	private Git initializeRepository(
			File repoDir,
			UserRepositoryEntity repoEntity,
			String accessToken)
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

		}

		String remoteUrl = String.format(
				"https://github.com/%s/%s.git",
				repoEntity.getRepoOwner(),
				repoEntity.getRepoName());

		return Git.cloneRepository()
				.setURI(remoteUrl)
				.setDirectory(repoDir)
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(
								"git",
								accessToken))
				.call();
	}

	/**
	 * GitHub APIでリポジトリへの投稿権限を確認する
	 */
	public boolean canPublish(
			UserRepositoryEntity repoEntity,
			String cipherKey)
			throws Exception {

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		String apiUrl = String.format(
				"https://api.github.com/repos/%s/%s",
				repoEntity.getRepoOwner(),
				repoEntity.getRepoName());

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.header(
						"Authorization",
						"Bearer " + accessToken)
				.header(
						"Accept",
						"application/vnd.github+json")
				.header(
						"X-GitHub-Api-Version",
						"2022-11-28")
				.GET()
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(
						request,
						HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			return false;
		}

		JsonNode permissions = new ObjectMapper()
				.readTree(response.body())
				.get("permissions");

		return permissions != null
				&& permissions.path("push").asBoolean(false);
	}

	/**
	 * 記事を非同期でGitHubへ投稿する
	 */
	@Async
	public void pushArticleAsync(
			UserRepositoryEntity repository,
			String cipherKey,
			Article article,
			boolean newArticle,
			Long workId,
			String slug) {

		try {
			article.setContent(
					articleImageUtil.convertImageUrl(
							article.getContent(),
							repository.getStorageBaseUrl()));

			pushArticle(
					repository,
					cipherKey,
					article,
					newArticle,
					slug);

			// GitHubへの投稿完了後にWorkを削除する
			articleWorkService.delete(
					workId,
					article.getUserId());

		} catch (Exception e) {

			System.err.println(
					"投稿処理に失敗しました: "
							+ e.getMessage());

			e.printStackTrace();

			// 投稿失敗した記事はエラー状態で残す
			articleWorkService.updateStatus(
					workId,
					2);
		}
	}
}
