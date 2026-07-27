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
				work.getCategoryId(),
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

		return new ArticlePublishResult(
				article,
				false);
	}

	/**
	 * 記事を非同期投稿する
	 *
	 * 画面で確定したslugをGitHub投稿まで引き継ぐ。
	 */
	public void publishAsync(
			UserRepositoryEntity repository,
			String cipherKey,
			Long workId,
			String slug) {

		ArticlePublishResult result = createOrUpdateArticle(
				workId,
				slug);

		// GitHubへ非同期投稿
		gitHubPushService.pushArticleAsync(
				repository,
				cipherKey,
				result.getArticle(),
				result.isNewArticle(),
				workId,
				slug);
	}
}