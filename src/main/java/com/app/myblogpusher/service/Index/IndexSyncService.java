/**
 * Hugoカテゴリーのインデックス情報をGitHubから取得し、DBへ同期するサービス
 *
 * CategoryRelationのカテゴリー経路を基準にGitHub上の_index.mdを読み込み、
 * indexテーブルへ新規登録または更新する。
 */
package com.app.myblogpusher.service.Index;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.Index;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.IndexRepository;
import com.app.myblogpusher.service.IndexEditService;
import com.app.myblogpusher.service.Github.GitHubPushService;

@Service
public class IndexSyncService {

	@Autowired
	private IndexEditService hugoIndexService;

	@Autowired
	private IndexRepository indexRepository;

	@Autowired
	private GitHubPushService gitHubPushService;

	/**
	 * GitHub上のカテゴリー_index.mdを非同期で取得し、indexテーブルへ同期する。
	 *
	 * CategoryRelationのカテゴリー経路を基準にGitHub上のファイルを読み込み、
	 * indexテーブルへ新規登録または更新する。
	 */
	@Async
	public void syncFromGitHub(
			Long userId, UserRepositoryEntity repo, String cipherKey) {

		try {
			List<CategoryOptionView> categoryPaths = hugoIndexService.findCategoryPaths(userId);

			int syncedCount = 0;

			for (CategoryOptionView category : categoryPaths) {
				String categoryPath = category.getCategoryPath();

				String markdown = gitHubPushService.readMarkdownFile(
						repo,
						cipherKey,
						categoryPath + "/_index.md");

				// GitHub上にファイルがない階層は同期対象にしない。
				if (markdown == null) {
					continue;
				}

				Optional<Index> existing = indexRepository
						.findByUserIdAndGroupId(
								userId,
								category.getGroupId());

				Index index = existing.orElseGet(Index::new);

				if (existing.isEmpty()) {
					index.setUserId(userId);
					index.setGroupId(category.getGroupId());
					index.setCreateUser(userId);
					index.setCreateDate(LocalDateTime.now());
				}

				// Front Matterと本文をDBへ反映する。
				index.setTitle(extractFrontMatterValue(markdown, "title"));
				index.setDescription(
						extractFrontMatterValue(markdown, "description"));
				index.setContent(markdown);
				index.setUpdateUser(userId);
				index.setUpdateDate(LocalDateTime.now());

				indexRepository.save(index);
				syncedCount++;
			}

			System.out.println(
					"カテゴリーインデックス同期完了: " + syncedCount + "件");

		} catch (IOException | GitAPIException e) {
			System.err.println(
					"カテゴリーインデックス同期に失敗しました: "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Markdownのフロントマターから指定キーの値を取得する
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
