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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.ArticleWork;
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
	@Transactional
	public boolean executePublish(
			Long workId,
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

		// 投稿前の記事だけを投稿中に更新する。
		// 先に確保することで、後続の更新処理も二重実行されないようにする。
		boolean started = articleWorkService.startPublishing(workId, userId);

		System.err.println(
		        "[PublishExecutionService] startPublishing"
		        + " workId=" + workId
		        + " result=" + started);

		if (!started) {
		    return true;
		}

		// 投稿対象のWorkを取得する。
		ArticleWork work = articleWorkService.findById(workId, userId);

		// 確認画面で確定したスラッグを保存する。
		articleWorkService.updateSlug(workId, slug, userId);

		// 投稿開始時に編集用ワークスペースを削除する。
		workspaceService.delete(userId);

		// DBのコミット完了後にGitHubへの投稿を非同期で開始する。
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						articlePublishService.publishAsync(
								repo,
								cipherKey,
								userId,
								workId,
								thumbnailUrl);
					}
				});

		return true;
	}
}
