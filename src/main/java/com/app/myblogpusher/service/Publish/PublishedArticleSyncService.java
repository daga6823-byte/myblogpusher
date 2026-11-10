/**
 * GitHub上の記事とArticleテーブルの同期を担当するサービス
 *
 * GitHubから取得した投稿済み記事をArticleテーブルへ反映し、
 * GitHubから削除された記事をDBから削除する。
 *
 * GitHub APIによる記事取得そのものはGitHubArticleServiceが担当する。
 */
package com.app.myblogpusher.service.Publish;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Publish.PublishedArticleSummaryDto;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.service.ApplicationErrorLogService;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Github.GitHubApiService;
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
	 *
	 * GitHubのファイルSHAを比較し、変更された記事だけ本文を取得する。
	 * 新規記事は必ず取得し、GitHubから削除された記事はDBから削除する。
	 */
	@Async
	public void syncArticles(
			UserRepositoryEntity repo,
			String cipherKey,
			Long userId) {

		try {

			// GitHubから記事のパスとSHAだけを取得
			List<GitHubApiService.GitHubMarkdownFile> files = gitHubArticleService.getPublishedArticleFiles(
					repo,
					cipherKey);

			Set<String> githubPaths = new HashSet<>();

			for (GitHubApiService.GitHubMarkdownFile file : files) {

				String hugoPath = file.path()
						.replaceFirst("^content/", "")
						.replaceFirst("\\.md$", "");

				githubPaths.add(hugoPath);

				Optional<Article> existing = articleRepository.findByUserIdAndHugoPath(
						userId,
						hugoPath);

				// 新規記事、またはGitHub上で変更された記事だけ本文を取得
				boolean needsSync = existing.isEmpty()
						|| !Objects.equals(
								existing.get().getGithubSha(),
								file.sha());

				if (!needsSync) {
					continue;
				}

				PublishedArticleSummaryDto summary = gitHubArticleService.getPublishedArticleSummary(
						repo,
						cipherKey,
						file.path());

				if (summary == null) {
					continue;
				}

				Long articleId = existing
						.map(Article::getArticleId)
						.orElse(null);

				Article article = articleService.saveFromGitHub(
				        articleId,
				        userId,
				        summary.getSlug(),
				        summary.getHugoPath(),
				        summary.getTitle(),
				        summary.getContent(),
				        summary.getUpdateDate(),
				        summary.getThumbnailUrl());

				// 本文の同期に成功した場合だけ同期情報を更新
				article.setGithubSha(file.sha());
				article.setSyncDate(LocalDateTime.now());

				articleRepository.save(article);
			}

			// GitHubから削除された記事をDBから削除
			List<Article> dbArticles = articleRepository.findByUserId(userId);

			for (Article dbArticle : dbArticles) {

				if (!githubPaths.contains(dbArticle.getHugoPath())) {
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