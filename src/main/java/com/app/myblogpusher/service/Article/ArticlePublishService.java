/**
 * 記事投稿処理を担当するサービス
 *
 * 下書きから投稿済み記事を作成・更新し、
 * GitHubへの投稿処理を呼び出す。
 */

package com.app.myblogpusher.service.Article;

import java.util.List;

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

		// ログインユーザーの投稿処理中の記事を取得する。
		List<ArticleWork> works = articleWorkService.findPublishing(userId);

		// 1記事ずつ投稿処理を実行する。
		for (ArticleWork work : works) {
			articlePublishWorkerService.publish(
					repo,
					cipherKey,
					work,
					thumbnailUrl);
		}
	}
}