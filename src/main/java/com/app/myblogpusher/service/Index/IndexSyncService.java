/**
* Hugoカテゴリーのインデックス情報をGitHubから取得し、DBへ同期するサービス
*
* カテゴリー経路を基準にGitHub上の_index.mdを読み込み、
* hugoPathを使ってindexテーブルへ新規登録または更新する。
*/
package com.app.myblogpusher.service.Index;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Index.IndexEntity;
import com.app.myblogpusher.facade.GitHubFacadeService;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.repository.Index.IndexRepository;
import com.app.myblogpusher.service.ApplicationErrorLogService;
import com.app.myblogpusher.service.Github.GitWorkspace;
import com.app.myblogpusher.service.Github.GitWorkspaceService;

@Service
public class IndexSyncService {

	@Autowired
	private IndexEditService hugoIndexService;

	@Autowired
	private IndexRepository indexRepository;

	@Autowired
	private GitHubFacadeService gitHubFacadeService;

	@Autowired
	private GitWorkspaceService gitWorkspaceService;
	
	/**
	 * リポジトリ単位でカテゴリーインデックス同期を排他制御するロック
	 */
	private final ConcurrentMap<Long, ReentrantLock> syncLocks =
	        new ConcurrentHashMap<>();

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	@Autowired
	private ApplicationErrorLogService applicationErrorLogService;

	/**
	 * GitHub上のカテゴリー_index.mdを非同期で取得し、indexテーブルへ同期する。
	 *
	 * リポジトリ単位で同期処理を排他制御し、最終同期日時から
	 * 24時間以内の場合はGitHub同期をスキップする。
	 *
	 * 同期が正常に完了した場合のみ、最終同期日時を更新する。
	 *
	 * @param userId ユーザーID
	 * @param repo GitHubリポジトリ情報
	 * @param cipherKey アクセストークン復号キー
	 */
	@Async
	public void syncFromGitHub(
	        Long userId, UserRepositoryEntity repo, String cipherKey) {

	    Long repoId = repo.getRepoId();

	    // 同一リポジトリの同期処理が重複しないように排他制御する。
	    ReentrantLock lock = syncLocks.computeIfAbsent(
	            repoId,
	            key -> new ReentrantLock());

	    lock.lock();

	    try {
	        // DBから最新の同期日時を取得する。
	        UserRepositoryEntity latestRepo = userRepositoryRepository
	                .findById(repoId)
	                .orElse(repo);

	        LocalDateTime lastSyncDate = latestRepo.getIndexSyncDate();

	        // 最終同期から24時間以内であればGitHub同期をスキップする。
	        if (lastSyncDate != null
	                && lastSyncDate.isAfter(LocalDateTime.now().minusHours(24))) {

	            System.out.println(
	                    "カテゴリーインデックス同期をスキップしました: repoId="
	                            + repoId
	                            + ", 最終同期日時="
	                            + lastSyncDate);

	            return;
	        }

	        try (GitWorkspace workspace = gitWorkspaceService.open(
	                latestRepo,
	                cipherKey)) {

	            List<CategoryOptionView> categoryPaths =
	                    hugoIndexService.findCategoryPaths(userId);

	            int syncedCount = 0;

	            for (CategoryOptionView category : categoryPaths) {

	                String categoryPath = category.getCategoryPath();

	                String markdown = gitHubFacadeService.readMarkdownFile(
	                        workspace,
	                        categoryPath + "/_index.md");

	                // GitHub上にファイルがない階層は同期対象にしない。
	                if (markdown == null) {
	                    continue;
	                }

	                Optional<IndexEntity> existing = indexRepository
	                        .findByUserIdAndHugoPath(
	                                userId,
	                                category.getCategoryPath());

	                IndexEntity index = existing.orElseGet(IndexEntity::new);

	                if (existing.isEmpty()) {
	                    index.setUserId(userId);
	                    index.setHugoPath(category.getCategoryPath());
	                    index.setCreateUser(userId);
	                    index.setCreateDate(LocalDateTime.now());
	                }

	                // Front Matterと本文をDBへ反映する。
	                index.setTitle(
	                        extractFrontMatterValue(markdown, "title"));
	                index.setContent(markdown);
	                index.setUpdateUser(userId);
	                index.setUpdateDate(LocalDateTime.now());

	                indexRepository.save(index);

	                syncedCount++;
	            }

	            System.out.println(
	                    "カテゴリーインデックス同期完了: "
	                            + syncedCount
	                            + "件");
	        }

	        // 全カテゴリーの同期が正常終了した場合のみ最終同期日時を更新する。
	        latestRepo.setIndexSyncDate(LocalDateTime.now());

	        userRepositoryRepository.save(latestRepo);

	    } catch (Exception e) {

	        // 非同期同期処理で発生した例外をDBへ記録する。
	        applicationErrorLogService.recordAsync(
	                e,
	                userId,
	                "IndexSyncService.syncFromGitHub");

	    } finally {

	        // 同期処理の終了時に排他ロックを解放する。
	        lock.unlock();
	    }
	}

	/**
	 * Markdownのフロントマターから指定キーの値を取得する。
	 */
	private String extractFrontMatterValue(String markdown, String key) {
		if (markdown == null || markdown.isBlank()) {
			return null;
		}

		String pattern = "(?m)^" + java.util.regex.Pattern.quote(key)
				+ "\\s*:\\s*[\"']?(.*?)[\"']?\\s*$";

		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(markdown);

		return matcher.find() ? matcher.group(1).trim() : null;
	}
}
