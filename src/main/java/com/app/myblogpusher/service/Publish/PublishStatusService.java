/**
 * 非同期投稿の状態判定を担当するサービス
 *
 * ArticleWorkが存在する間は投稿中または失敗を返し、
 * Work削除後はArticleの公開ステータスと投稿先リポジトリ情報から公開記事のURLを組み立てる。
 */
package com.app.myblogpusher.service.Publish;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.enums.ArticleStatus;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Article.ArticleWorkService;

@Service
public class PublishStatusService {

	private static final int WORK_STATUS_FAILED = 2;

	private final ArticleWorkService articleWorkService;
	private final ArticleService articleService;
	private final UserRepositoryRepository userRepositoryRepository;

	/**
	 * 投稿状態の判定結果。urlはPUBLISHEDの場合のみ設定される。
	 */
	public record PublishStatusResult(String status, String url) {

		static PublishStatusResult of(String status) {
			return new PublishStatusResult(status, null);
		}
	}

	/**
	 * 投稿状態の判定に必要な依存関係を設定する。
	 */
	public PublishStatusService(
			ArticleWorkService articleWorkService,
			ArticleService articleService,
			UserRepositoryRepository userRepositoryRepository) {
		this.articleWorkService = articleWorkService;
		this.articleService = articleService;
		this.userRepositoryRepository = userRepositoryRepository;
	}

	/**
	 * 投稿状態を判定する。
	 *
	 * @return PENDING / FAILED / PUBLISHED のいずれかを持つ判定結果
	 */
	public PublishStatusResult getStatus(Long userId, Long workId, String slug) {

		// 投稿対象のWorkが残っていれば、投稿中または失敗。
		var works = articleWorkService.findByUserId(userId);

		for (var work : works) {
			if (workId.equals(work.getWorkId())) {
				if (work.getStatus() != null && work.getStatus() == WORK_STATUS_FAILED) {
					return PublishStatusResult.of("FAILED");
				}

				return PublishStatusResult.of("PENDING");
			}
		}

		// Work削除後は、公開済みArticleを確認する。
		var article = articleService.findBySlug(userId, slug);

		if (article == null
				|| article.getStatus() != ArticleStatus.PUBLISHED) {
			return PublishStatusResult.of("PENDING");
		}

		var repoOpt = userRepositoryRepository.findByUserId(userId);

		if (repoOpt.isEmpty()) {
			return PublishStatusResult.of("FAILED");
		}

		String hugoPath = article.getHugoPath();

		if (hugoPath == null || hugoPath.isBlank()) {
			return PublishStatusResult.of("FAILED");
		}

		var repo = repoOpt.get();
		String baseUrl = buildBaseUrl(repo.getRepoOwner(), repo.getRepoName());

		hugoPath = hugoPath.replaceAll("^/+", "").replaceAll("/+$", "");

		return new PublishStatusResult("PUBLISHED", baseUrl + hugoPath + "/");
	}

	/**
	 * GitHub Pagesの公開ベースURLを組み立てる。
	 *
	 * リポジトリ名が「オーナー名.github.io」の場合はユーザーサイト、
	 * それ以外はプロジェクトサイトとして扱う。
	 */
	private String buildBaseUrl(String repoOwner, String repoName) {

		if (repoName.equalsIgnoreCase(repoOwner + ".github.io")) {
			return "https://" + repoName + "/";
		}

		return "https://" + repoOwner + ".github.io/" + repoName + "/";
	}
}