/**
 * 記事のGitHub投稿を非同期で実行するサービス
 *
 * 画像URL変換、GitHubPushServiceによる投稿、
 * 成功時のWork削除、失敗時のエラー状態保存という投稿の流れを担当する。
 */

package com.app.myblogpusher.service.Github;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.util.ArticleImageUtil;

@Service
public class ArticlePublishAsyncService {

	private final GitHubPushService gitHubPushService;
	private final ArticleImageUtil articleImageUtil;
	private final ArticleWorkService articleWorkService;

	public ArticlePublishAsyncService(
			GitHubPushService gitHubPushService,
			ArticleImageUtil articleImageUtil,
			ArticleWorkService articleWorkService) {

		this.gitHubPushService = gitHubPushService;
		this.articleImageUtil = articleImageUtil;
		this.articleWorkService = articleWorkService;
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

			gitHubPushService.pushArticle(
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