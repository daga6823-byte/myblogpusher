/**
 * 記事投稿処理を担当するサービス
 *
 * 下書きから投稿済み記事を作成・更新し、
 * GitHubへの投稿処理を呼び出す。
 */

package com.app.myblogpusher.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.ArticlePublishResult;
import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserRepositoryEntity;

@Service
public class ArticlePublishService {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private GitHubPushService gitHubPushService;

	/**
	 * 下書きから投稿済み記事を作成または更新する
	 */
	private ArticlePublishResult createOrUpdateArticle(Long workId) {

		ArticleWork work = articleWorkService.findById(workId);

		// 新規投稿
		if (work.getArticleId() == null) {
			return new ArticlePublishResult(
					articleService.createFromWork(work),
					true);
		}

		Article article = articleService.findById(work.getArticleId());

		return new ArticlePublishResult(
				articleService.updateFromWork(article, work),
				false);
	}

	/**
	 * 記事を非同期投稿する
	 */
	public void publishAsync(
			UserRepositoryEntity repository,
			String cipherKey,
			Long workId) {

		// 投稿済み記事を作成または更新
		ArticlePublishResult result =
				createOrUpdateArticle(workId);

		// GitHubへ非同期投稿
		gitHubPushService.pushArticleAsync(
				repository,
				cipherKey,
				result.getArticle(),
				result.isNewArticle(),
				workId);

	}

}