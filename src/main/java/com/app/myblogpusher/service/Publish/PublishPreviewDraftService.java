/**
 * 投稿確認画面へ進む前のワーク保存を担当するサービス
 *
 * 記事とカテゴリー情報をワークテーブルへ保存し、
 * 保存成功後に一時ワークスペースを削除する。
 */
package com.app.myblogpusher.service.Publish;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.service.Article.ArticleWorkspaceService;
import com.app.myblogpusher.util.ArticleSaveUtil;

@Service
public class PublishPreviewDraftService {

	private final ArticleSaveUtil articleSaveUtil;
	private final ArticleWorkspaceService workspaceService;

	/**
	 * ワーク保存に必要な依存関係を設定する。
	 */
	public PublishPreviewDraftService(
			ArticleSaveUtil articleSaveUtil,
			ArticleWorkspaceService workspaceService) {
		this.articleSaveUtil = articleSaveUtil;
		this.workspaceService = workspaceService;
	}

	/**
	 * 記事をワークテーブルへ保存する。
	 *
	 * @return 保存したワークID。保存に失敗した場合はnull
	 */
	public Long saveDraft(
			Long workId,
			String categorySelect,
			String newCategoryName,
			String newCategoryDisplayName,
			String title,
			String content,
			Long userId) {

		Long savedWorkId = articleSaveUtil.doSaveDraft(
				workId,
				categorySelect,
				newCategoryName,
				newCategoryDisplayName,
				title,
				content,
				userId);

		if (savedWorkId == null) {
			return null;
		}

		// ワークテーブルへの保存成功後、一時ワークスペースを削除する。
		workspaceService.delete(userId);

		return savedWorkId;
	}
}