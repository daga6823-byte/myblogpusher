/**
 * 記事をGitHubリポジトリにMarkdownファイルとしてプッシュするサービス
 *
 * Git操作、認証、非同期投稿処理を担当する。
 * Hugo用Markdown生成はHugoArticleServiceへ委譲する。
 */

package com.app.myblogpusher.service.Github;

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
import com.app.myblogpusher.service.HugoArticleService;
import com.app.myblogpusher.service.TokenCipherService;
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
			Article existingArticle,
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

			System.out.println("=== GitHubPushService.pushArticle ===");
			System.out.println("newArticle       = " + newArticle);
			System.out.println("article.articleId = " + article.getArticleId());
			System.out.println("article.slug      = " + article.getSlug());
			System.out.println("article.hugoPath  = " + article.getHugoPath());

			if (existingArticle != null) {
				System.out.println("existingArticle.articleId = "
						+ existingArticle.getArticleId());
				System.out.println("existingArticle.hugoPath  = "
						+ existingArticle.getHugoPath());
			} else {
				System.out.println("existingArticle = null");
			}

			// 既存記事のカテゴリー変更などでHugoパスが変わった場合、
			// 既存ファイルを削除せず、新しいパスへ移動する。
			if (existingArticle != null
					&& existingArticle.getHugoPath() != null
					&& !existingArticle.getHugoPath().equals(article.getHugoPath())) {

				System.out.println("=== Hugoパス変更検出 ===");
				System.out.println("旧パス: " + existingArticle.getHugoPath());
				System.out.println("新パス: " + article.getHugoPath());

				hugoArticleService.moveArticle(
						git,
						repoPath,
						existingArticle.getHugoPath(),
						article.getHugoPath());
			}

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
	 * GitHubリポジトリから既存のHugoインデックスファイルを読み込む。
	 *
	 * リポジトリを初期化して最新状態へ更新し、
	 * ファイルが存在しない場合はnullを返す。
	 *
	 * @param repoEntity 接続先リポジトリ情報
	 * @param cipherKey アクセストークンの復号キー
	 * @param relativePath contentディレクトリからの相対パス
	 * @return ファイル内容。存在しない場合はnull
	 */
	public String readMarkdownFile(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String relativePath)
			throws IOException, GitAPIException {

		java.nio.file.Path contentDir = java.nio.file.Paths.get("content");
		java.nio.file.Path relativeFile = java.nio.file.Paths.get(relativePath).normalize();

		// _index.md以外やリポジトリ外へのアクセスを拒否する。
		if (relativeFile.isAbsolute()
				|| relativeFile.startsWith("..")
				|| relativeFile.getNameCount() < 2
				|| !"_index.md".equals(relativeFile.getFileName().toString())) {
			throw new IllegalArgumentException(
					"インデックスファイルのパスが不正です: " + relativePath);
		}

		java.nio.file.Path filePath = contentDir.resolve(relativeFile).normalize();

		if (!filePath.startsWith(contentDir)) {
			throw new IllegalArgumentException(
					"リポジトリ外のファイルは読み込めません。");
		}

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		String repoPath = System.getProperty("java.io.tmpdir")
				+ "/myblogpusher_" + repoEntity.getRepoId();

		File repoDir = new File(repoPath);
		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}

		Git git = initializeRepository(repoDir, repoEntity, accessToken);

		try {
			java.nio.file.Path targetPath = java.nio.file.Paths.get(repoPath).resolve(filePath);

			if (!java.nio.file.Files.exists(targetPath)) {
				return null;
			}

			return java.nio.file.Files.readString(
					targetPath,
					java.nio.charset.StandardCharsets.UTF_8);

		} finally {
			git.close();
		}
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
					null,
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

			String errorCode = resolveErrorCode(e);

			// 投稿失敗した記事はエラー状態で残し、
			// エラーコードをArticleWorkへ保存する。
			articleWorkService.updateStatus(
					workId,
					2,
					errorCode);
		}
	}

	/**
	 * HugoのMarkdownファイルを保存し、GitHubへプッシュする。
	 *
	 * 記事投稿とは独立して、カテゴリーの_index.mdなどを更新する。
	 * リポジトリの初期化・認証には既存の処理を利用する。
	 *
	 * @param repoEntity 接続先リポジトリ情報
	 * @param cipherKey アクセストークンの復号キー
	 * @param relativePath contentディレクトリからの相対パス
	 * @param content 保存するMarkdown本文
	 * @param commitMessage コミットメッセージ
	 */
	public void pushMarkdownFile(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String relativePath,
			String content,
			String commitMessage)
			throws IOException, GitAPIException {

		// インデックスファイル以外への書き込みを防ぐ。
		java.nio.file.Path contentDir = java.nio.file.Paths.get("content");
		java.nio.file.Path relativeFile = java.nio.file.Paths.get(relativePath).normalize();

		if (relativeFile.isAbsolute()
				|| relativeFile.startsWith("..")
				|| relativeFile.getNameCount() < 2
				|| !"_index.md".equals(relativeFile.getFileName().toString())) {
			throw new IllegalArgumentException(
					"インデックスファイルのパスが不正です: " + relativePath);
		}

		java.nio.file.Path filePath = contentDir.resolve(relativeFile).normalize();

		if (!filePath.startsWith(contentDir)) {
			throw new IllegalArgumentException(
					"リポジトリ外への書き込みは許可されていません。");
		}

		// 既存の認証・リポジトリ初期化処理を再利用する。
		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		String repoPath = System.getProperty("java.io.tmpdir")
				+ "/myblogpusher_" + repoEntity.getRepoId();

		File repoDir = new File(repoPath);
		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}

		Git git = initializeRepository(repoDir, repoEntity, accessToken);

		try {
			java.nio.file.Path targetPath = java.nio.file.Paths
					.get(repoPath)
					.resolve(filePath);

			// 内容が変わっていなければ不要なコミット・プッシュを行わない。
			if (java.nio.file.Files.exists(targetPath)
					&& java.nio.file.Files.readString(
							targetPath,
							java.nio.charset.StandardCharsets.UTF_8)
							.equals(content)) {
				return;
			}

			java.nio.file.Files.createDirectories(targetPath.getParent());
			java.nio.file.Files.writeString(
					targetPath,
					content,
					java.nio.charset.StandardCharsets.UTF_8);

			git.add().addFilepattern(filePath.toString().replace('\\', '/')).call();

			git.commit()
					.setMessage(commitMessage)
					.setAuthor("Myblogpusher", "noreply@myblogpusher.local")
					.call();

			git.push()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider("git", accessToken))
					.call();

		} finally {
			git.close();
		}
	}

	/**
	 * 投稿失敗時の例外からエラーコードを判定する。
	 */
	private String resolveErrorCode(Exception e) {

		String message = e.getMessage();

		if (message != null
				&& message.contains(
						"git-receive-pack not permitted")) {

			return "GITHUB_PERMISSION_ERROR";
		}

		return "GITHUB_PUSH_ERROR";
	}
}
