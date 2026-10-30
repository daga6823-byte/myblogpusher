/**
 * 記事投稿処理を担当するサービス
 *
 * 下書きから投稿済み記事を作成・更新し、
 * GitHubへの投稿処理を呼び出す。
 */

package com.app.myblogpusher.service.Article;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.ArticleWork;

@Service
public class ArticlePublishService {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticlePublishWorkerService articlePublishWorkerService;

	/**
	 * 投稿中の記事をまとめて非同期投稿する。
	 *
	 * ArticleWork.status = 1 の記事を対象とし、
	 * 1記事分の投稿処理はArticlePublishWorkerServiceへ委譲する。
	 */
	@Async
	public void publishAsync(
			UserRepositoryEntity repo,
			String cipherKey,
			Long userId,
			Long workId,
			String thumbnailUrl) {

		// 投稿ボタンで指定されたWorkだけを取得する。
		ArticleWork work = articleWorkService.findById(
				workId,
				userId);

		// 指定された1記事の投稿処理を実行する。
		if (work.getStatus() == null || work.getStatus() != 1) {
			return;
		}

		articlePublishWorkerService.publish(
				repo,
				cipherKey,
				work,
				thumbnailUrl);
	}
}