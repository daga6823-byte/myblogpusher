/**
 * 記事投稿処理を担当するサービス
 *
 * 下書きから投稿済み記事を作成・更新し、
 * GitHubへの投稿処理を呼び出す。
 */

package com.app.myblogpusher.service.Article;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.service.HugoArticleService;
import com.app.myblogpusher.service.Github.GitHubPushService;

@Service
public class ArticlePublishService {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private GitHubPushService gitHubPushService;

	@Autowired
	private HugoArticleService hugoArticleService;

	/**
	 * 投稿中の記事をまとめて非同期投稿する
	 *
	 * ArticleWork.status = 1 の記事を対象とする。
	 */
	@Async
	public void publishAsync(
			UserRepositoryEntity repository,
			String cipherKey,
			Long userId) {

		List<ArticleWork> works = articleWorkService.findPublishing(userId);

		for (ArticleWork work : works) {

			try {

				// 投稿処理中(status=1)のWorkだけを投稿する。
				// 非同期処理開始後にstatusが変更された場合の誤投稿を防ぐ。
				if (work.getStatus() == null
						|| work.getStatus() != 1) {

					continue;
				}

				// GitHub APIで投稿可能か確認する
				if (!gitHubPushService.canPublish(
						repository,
						cipherKey)) {

					articleWorkService.updateStatus(
							work.getWorkId(),
							2,
							"GitHub APIで投稿権限を確認できませんでした。");

					continue;
				}

				String hugoPath = hugoArticleService.buildArticlePath(
						work.getCategoryGroupId(),
						work.getSlug());

				// ArticleWork.articleIdを基準に元の記事を取得する。
				// カテゴリー変更後のhugoPathではなく、元の記事そのものを特定する。
				Article existingArticle = null;

				if (work.getArticleId() != null) {
					existingArticle = articleService.findById(
							work.getArticleId());
				}

				// GitHub投稿用のArticleをメモリ上だけで作成する
				Article article = new Article();

				article.setUserId(work.getUserId());
				article.setCategoryGroupId(work.getCategoryGroupId());
				article.setTitle(work.getTitle());
				article.setSlug(work.getSlug());
				article.setHugoPath(hugoPath);
				article.setContent(work.getContent());

				// GitHub投稿を実行する
				// この時点ではArticleテーブルには保存しない
				gitHubPushService.pushArticle(
						repository,
						cipherKey,
						article,
						existingArticle,
						existingArticle == null,
						work.getSlug());

				// GitHub投稿成功後にArticleを作成・更新する。
				// Articleの重複整理、PUBLISHEDへの変更、Work削除まで
				// DBトランザクション内でまとめて実行する。
				articleService.completePublish(
						work,
						work.getSlug());

			} catch (Exception e) {
				System.err.println(
						"投稿処理に失敗しました: "
								+ e.getMessage());
				e.printStackTrace();

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);

				e.printStackTrace(pw);

				String errorMessage = sw.toString();

				// 投稿失敗した記事はエラー状態で残す。
				// 例外メッセージだけでなく、スタックトレースもDBへ保存する。
				articleWorkService.updateStatus(
						work.getWorkId(),
						2,
						errorMessage);
			}
		}
	}
}