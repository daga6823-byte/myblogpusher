/**
 * 記事投稿処理を担当するサービス
 *
 * 下書きから投稿済み記事を作成・更新し、
 * GitHubへの投稿処理を呼び出す。
 */

package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Article.ArticlePublishResult;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.service.GitHubPushService;
import com.app.myblogpusher.service.HugoArticleService;

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
	 * 下書きから投稿済み記事を作成または更新する
	 *
	 * slugは投稿確認画面で編集された最終値を使用する。
	 */
	private ArticlePublishResult createOrUpdateArticle(
			Long workId,
			String slug) {

		ArticleWork work = articleWorkService.findById(workId);

		String hugoPath = hugoArticleService.buildArticlePath(
				work.getCategoryGroupId(),
				slug);

		System.out.println("work.userId = " + work.getUserId());
		System.out.println("slug = " + slug);
		System.out.println("hugoPath = " + hugoPath);

		Article article = articleService.findByHugoPath(
				work.getUserId(),
				hugoPath);

		System.out.println("find result = " + article);

		// 新規投稿
		if (article == null) {

			article = articleService.createFromWork(
					work,
					slug);

			return new ArticlePublishResult(
					article,
					true);
		}

		// 更新
		article = articleService.updateFromWork(
				article,
				work,
				slug);

		// 古い記事の作成日時を投稿時に正規化する
		if (article.getCreateDate() != null) {

			LocalDateTime normalizedCreateDate = article.getCreateDate().withNano(0);

			article.setCreateDate(normalizedCreateDate);

			article = articleService.save(article);
		}

		return new ArticlePublishResult(
				article,
				false);
	}

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

				// 既存Articleの有無だけ確認する
				// ここではArticleテーブルを変更しない
				Article existingArticle = articleService.findByHugoPath(
						work.getUserId(),
						hugoPath);

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
						existingArticle == null,
						work.getSlug());

				// GitHub投稿成功後にArticleを作成・更新する
				ArticlePublishResult result = createOrUpdateArticle(
						work.getWorkId(),
						work.getSlug());

				articleService.updateStatus(
						result.getArticle().getArticleId(),
						com.app.myblogpusher.enums.ArticleStatus.PUBLISHED);

				// 投稿完了後にWorkを削除する
				articleWorkService.delete(
						work.getWorkId(),
						userId);

			} catch (Exception e) {

				System.err.println(
						"投稿処理に失敗しました: "
								+ e.getMessage());

				e.printStackTrace();

				// 投稿失敗した記事はエラー状態で残す
				articleWorkService.updateStatus(
				        work.getWorkId(),
				        2,
				        e.getMessage());
			}
		}
	}
}