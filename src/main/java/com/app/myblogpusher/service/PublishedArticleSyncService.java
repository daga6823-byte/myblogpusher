/**
 * GitHub上の記事とArticleテーブルの同期を担当するサービス
 *
 * GitHubから取得した投稿済み記事をArticleテーブルへ反映し、
 * GitHubから削除された記事をDBから削除する。
 *
 * GitHub APIによる記事取得そのものはGitHubArticleServiceが担当する。
 */
package com.app.myblogpusher.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Publish.PublishedArticleSummaryDto;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Github.GitHubArticleService;

@Service
public class PublishedArticleSyncService {

	@Autowired
	private GitHubArticleService gitHubArticleService;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private ArticleRepository articleRepository;
	
	@Autowired
	private ApplicationErrorLogService applicationErrorLogService;

	/**
	 * GitHub上の記事をArticleテーブルへ同期する。
	 */
	@Async
	public void syncArticles(
			UserRepositoryEntity repo,
			String cipherKey,
			Long userId) {

		try {

			List<PublishedArticleSummaryDto> articles = gitHubArticleService.getPublishedArticles(
					repo,
					cipherKey,
					null);

			for (PublishedArticleSummaryDto summary : articles) {

				Optional<Article> existing = articleRepository.findByUserIdAndHugoPath(
						userId,
						summary.getHugoPath());

				Long articleId = existing
						.map(Article::getArticleId)
						.orElse(null);

				if (existing.isEmpty()) {

					articleService.saveFromGitHub(
							articleId,
							userId,
							summary.getSlug(),
							summary.getHugoPath(),
							summary.getTitle(),
							summary.getContent(),
							summary.getUpdateDate());

				} else {

					Article dbArticle = existing.get();

					if (summary.getUpdateDate() != null
							&& (dbArticle.getUpdateDate() == null
									|| summary.getUpdateDate()
											.isAfter(
													dbArticle.getUpdateDate()))) {

						articleService.saveFromGitHub(
								articleId,
								userId,
								summary.getSlug(),
								summary.getHugoPath(),
								summary.getTitle(),
								summary.getContent(),
								summary.getUpdateDate());
					}
				}
			}

			List<Article> dbArticles = articleRepository.findByUserId(userId);

			for (Article dbArticle : dbArticles) {

				boolean exists = articles.stream()
						.anyMatch(
								github -> github.getHugoPath()
										.equals(
												dbArticle.getHugoPath()));

				if (!exists) {

					articleService.deleteById(
							dbArticle.getArticleId());
				}
			}

		} catch (Exception e) {

			applicationErrorLogService.recordAsync(
					e,
					userId,
					"PublishedArticleSyncService.syncArticles");
		}
	}
}