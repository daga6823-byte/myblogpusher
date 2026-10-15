/**
 * 投稿確定後の処理を担当するサービス
 *
 * 投稿先リポジトリの確認、投稿カテゴリーとスラッグの更新、
 * 投稿中ステータスへの変更、編集用ワークスペースの削除、
 * 非同期のGitHub投稿開始を担当する。
 */
package com.app.myblogpusher.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.Article.ArticlePublishService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Article.ArticleWorkspaceService;

@Service
public class PublishExecutionService {

	private final UserRepositoryRepository userRepositoryRepository;
	private final ArticlePublishService articlePublishService;
	private final ArticleWorkService articleWorkService;
	private final ArticleWorkspaceService workspaceService;

	/**
	 * 投稿確定処理に必要な依存関係を設定する。
	 */
	public PublishExecutionService(
			UserRepositoryRepository userRepositoryRepository,
			ArticlePublishService articlePublishService,
			ArticleWorkService articleWorkService,
			ArticleWorkspaceService workspaceService) {

		this.userRepositoryRepository = userRepositoryRepository;
		this.articlePublishService = articlePublishService;
		this.articleWorkService = articleWorkService;
		this.workspaceService = workspaceService;
	}

	/**
	 * 投稿を確定し、非同期のGitHub投稿処理を開始する。
	 *
	 * @return リポジトリが設定され、投稿処理を開始できた場合はtrue
	 */
	public boolean executePublish(
			Long workId,
			Long categoryId,
			String slug,
			String thumbnailUrl,
			Long userId,
			String cipherKey) {

		// 投稿先リポジトリの設定を確認する。
		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);

		if (repoOpt.isEmpty()) {
			return false;
		}

		UserRepositoryEntity repo = repoOpt.get();

		// 確認画面で確定したカテゴリーグループIDとスラッグを保存する。
		articleWorkService.updateCategoryGroupId(workId, categoryId);
		articleWorkService.updateSlug(workId, slug);

		// 投稿処理中のステータスに更新する。
		articleWorkService.updateStatus(workId, 1);

		// 投稿開始時に編集用ワークスペースを削除する。
		workspaceService.delete(userId);

		// GitHubへの投稿を非同期で開始する。
		articlePublishService.publishAsync(
				repo,
				cipherKey,
				userId,
				workId,
				thumbnailUrl);

		return true;
	}
}
