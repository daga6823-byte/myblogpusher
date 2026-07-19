/**
 * 記事をGitHubリポジトリにMarkdownファイルとしてプッシュするサービス
 *
 * Git操作、認証、非同期投稿処理を担当する。
 * Hugo用Markdown生成はHugoArticleServiceへ委譲する。
 */

package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.enums.ArticleStatus;
import com.app.myblogpusher.util.ArticleImageUtil;

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
			boolean newArticle)
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
					article);

			String action = article.getPublishDate()
					.equals(article.getUpdateDate())
							? "Add article: "
							: "Update article: ";

			String commitMessage =
					newArticle
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

			articleService.updateStatus(
					article.getArticleId(),
					ArticleStatus.PUBLISHED);

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

	@Async
	public void pushArticleAsync(
			UserRepositoryEntity repository,
			String cipherKey,
			Article article,
			boolean newArticle,
			Long workId) {

		try {

			article.setContent(
					articleImageUtil.convertImageUrl(
							article.getContent(),
							repository.getStorageBaseUrl()));

			pushArticle(
					repository,
					cipherKey,
					article,
					newArticle);

			articleWorkService.delete(
			        workId,
			        article.getUserId());

		} catch (Exception e) {

			System.err.println(
					"投稿処理に失敗しました: "
							+ e.getMessage());

			e.printStackTrace();
		}
	}
}
