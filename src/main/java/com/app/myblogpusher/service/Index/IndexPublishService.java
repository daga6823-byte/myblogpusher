
/**
 * カテゴリーインデックスのGitHub投稿処理を担当するサービス
 *
 * 投稿権限の確認、GitHubへの反映、公開済みデータの更新、
 * 下書きの削除、失敗時のエラー状態管理を行う。
 */
package com.app.myblogpusher.service.Index;

import java.io.IOException;
import java.time.LocalDateTime;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Index.Index;
import com.app.myblogpusher.entity.Index.IndexWork;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.repository.Index.IndexRepository;
import com.app.myblogpusher.service.Facade.GitHubFacadeService;

@Service
public class IndexPublishService {

	private final UserRepositoryRepository userRepositoryRepository;
	private final CategoryRelationRepository categoryRelationRepository;
	private final IndexRepository indexRepository;
	private final IndexWorkService indexWorkService;
	private final IndexEditService hugoIndexService;
	private final GitHubFacadeService gitHubFacadeService;
	private final IndexWorkspaceService indexWorkspaceService;

	public IndexPublishService(
			UserRepositoryRepository userRepositoryRepository,
			CategoryRelationRepository categoryRelationRepository,
			IndexRepository indexRepository,
			IndexWorkService indexWorkService,
			IndexEditService hugoIndexService,
			GitHubFacadeService gitHubFacadeService,
			IndexWorkspaceService indexWorkspaceService) {

		this.userRepositoryRepository = userRepositoryRepository;
		this.categoryRelationRepository = categoryRelationRepository;
		this.indexRepository = indexRepository;
		this.indexWorkService = indexWorkService;
		this.hugoIndexService = hugoIndexService;
		this.gitHubFacadeService = gitHubFacadeService;
		this.indexWorkspaceService = indexWorkspaceService;
	}

	/**
	 * 投稿確認画面で確定したインデックスをGitHubへ反映する。
	 *
	 * 成功時はindexを更新してindex_workを削除する。
	 * 失敗時は下書きを残し、再試行できるようエラー状態にする。
	 */
	public void publish(
			Long userId,
			String cipherKey,
			Long groupId)
			throws IOException, GitAPIException {

		IndexWork work = indexWorkService
				.findByUserIdAndGroupId(userId, groupId)
				.orElseThrow(() -> new IllegalArgumentException(
						"反映するインデックスが見つかりません。"));

		try {
			// 確定時にも、対象カテゴリーの編集権限を再確認する。
			boolean ownsCategoryPath = hugoIndexService
					.findCategoryPaths(userId)
					.stream()
					.anyMatch(category -> category.getGroupId().equals(groupId));

			if (!ownsCategoryPath) {
				throw new IllegalArgumentException(
						"指定されたカテゴリー階層を編集する権限がありません。");
			}

			UserRepositoryEntity repository = userRepositoryRepository
					.findByUserId(userId)
					.orElseThrow(() -> new IllegalArgumentException(
							"リポジトリが設定されていません。"));

			CategoryRelation relation = categoryRelationRepository
					.findByGroupId(groupId)
					.stream()
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(
							"カテゴリー階層が見つかりません。"));

			String categoryPath = relation.getCategoryPath();

			if (categoryPath == null || categoryPath.isBlank()) {
				throw new IllegalArgumentException(
						"カテゴリー経路が設定されていません。");
			}

			// GitHub反映中の状態にする。
			indexWorkService.updateStatus(work.getWorkId(), 1);

			// GitHub連携の詳細はファサードへ委譲する。
			gitHubFacadeService.pushMarkdownFile(
					repository,
					cipherKey,
					categoryPath + "/_index.md",
					work.getContent(),
					"Update category index: " + categoryPath);

			// GitHub反映成功後に公開済みデータを登録・更新する。
			Index index = indexRepository
					.findByUserIdAndGroupId(userId, groupId)
					.orElseGet(() -> {
						Index newIndex = new Index();
						newIndex.setUserId(userId);
						newIndex.setGroupId(groupId);
						newIndex.setCreateUser(userId);
						newIndex.setCreateDate(LocalDateTime.now());
						return newIndex;
					});

			index.setTitle(work.getTitle());
			index.setContent(work.getContent());
			index.setUpdateUser(userId);
			index.setUpdateDate(LocalDateTime.now());

			indexRepository.save(index);

			// GitHubとDBへの反映が成功してから下書きと自動保存データを削除する。
			indexWorkService.delete(work.getWorkId(), userId);
			indexWorkspaceService.delete(userId);

		} catch (IOException | GitAPIException | RuntimeException e) {
			// DB更新失敗も含め、下書きを残して再試行可能な状態にする。
			indexWorkService.updateStatus(
					work.getWorkId(), 2, e.getMessage());

			throw e;
		}
	}
}
