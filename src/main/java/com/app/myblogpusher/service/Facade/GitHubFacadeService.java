/**
 * GitHub連携機能の窓口となるファサード
 *
 * 記事のプッシュ、_index.mdの読み書き、投稿権限の確認、非同期投稿を
 * 1つのクラスにまとめ、呼び出し側が個々のサービスを意識せずに済むようにする。
 * 自身はロジックを持たず、各サービスへ委譲するだけとする。
 */

package com.app.myblogpusher.service.Facade;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.service.Github.ArticlePublishAsyncService;
import com.app.myblogpusher.service.Github.GitHubMarkdownFileService;
import com.app.myblogpusher.service.Github.GitHubPermissionService;
import com.app.myblogpusher.service.Github.GitHubPushService;
import com.app.myblogpusher.service.Github.GitWorkspace;

@Service
public class GitHubFacadeService {

	private final GitHubPushService gitHubPushService;
	private final GitHubMarkdownFileService gitHubMarkdownFileService;
	private final GitHubPermissionService gitHubPermissionService;
	private final ArticlePublishAsyncService articlePublishAsyncService;

	public GitHubFacadeService(
			GitHubPushService gitHubPushService,
			GitHubMarkdownFileService gitHubMarkdownFileService,
			GitHubPermissionService gitHubPermissionService,
			ArticlePublishAsyncService articlePublishAsyncService) {

		this.gitHubPushService = gitHubPushService;
		this.gitHubMarkdownFileService = gitHubMarkdownFileService;
		this.gitHubPermissionService = gitHubPermissionService;
		this.articlePublishAsyncService = articlePublishAsyncService;
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

		gitHubPushService.pushArticle(
				repoEntity,
				cipherKey,
				article,
				existingArticle,
				newArticle,
				slug);
	}

	/**
	 * 記事を非同期でGitHubへ投稿する。
	 *
	 * @Asyncは委譲先のArticlePublishAsyncServiceに付いているため、
	 * ファサード側には付けない。
	 */
	public void pushArticleAsync(
			UserRepositoryEntity repository,
			String cipherKey,
			Article article,
			boolean newArticle,
			Long workId,
			String slug) {

		articlePublishAsyncService.pushArticleAsync(
				repository,
				cipherKey,
				article,
				newArticle,
				workId,
				slug);
	}

	/**
	 * GitHubリポジトリから既存のHugoインデックスファイルを読み込む。
	 * ファイルが存在しない場合はnullを返す。
	 */
	public String readMarkdownFile(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String relativePath)
			throws IOException, GitAPIException {

		return gitHubMarkdownFileService.readMarkdownFile(
				repoEntity,
				cipherKey,
				relativePath);
	}

	/**
	 * HugoのMarkdownファイルを保存し、GitHubへプッシュする。
	 */
	public void pushMarkdownFile(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String relativePath,
			String content,
			String commitMessage)
			throws IOException, GitAPIException {

		gitHubMarkdownFileService.pushMarkdownFile(
				repoEntity,
				cipherKey,
				relativePath,
				content,
				commitMessage);
	}

	/**
	 * GitHub APIでリポジトリへの投稿権限を確認する
	 */
	public boolean canPublish(
			UserRepositoryEntity repoEntity,
			String cipherKey)
			throws Exception {

		return gitHubPermissionService.canPublish(
				repoEntity,
				cipherKey);
	}

	/**
	 * 開いているGit作業領域からHugoインデックスファイルを読み込む。
	 */
	public String readMarkdownFile(
			GitWorkspace workspace,
			String relativePath) throws IOException {

		return gitHubMarkdownFileService.readMarkdownFile(
				workspace,
				relativePath);
	}
}