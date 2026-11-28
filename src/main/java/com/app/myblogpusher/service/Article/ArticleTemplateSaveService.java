
/**
 * 記事ごとのテンプレート設定を一括保存するサービス
 *
 * テンプレート全体設定とセルごとの設定を同一トランザクションで保存する。
 * 下書きと公開記事の保存先の振り分けも担当する。
 */
package com.app.myblogpusher.service.Article;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Article.ArticleTemplateCellEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleTemplateSaveService {

	private final ArticleTemplateService articleTemplateService;
	private final ArticleTemplateCellService articleTemplateCellService;

	/**
	 * テンプレート全体設定とセル情報を一括保存する
	 *
	 * workIdがある場合は下書きに保存し、
	 * workIdがなくarticleIdがある場合は公開記事に保存する。
	 */
	@Transactional
	public void save(
			Long userId,
			Long workId,
			Long articleId,
			Long templateId,
			Long backgroundImageAssetId,
			String backgroundColor,
			List<ArticleTemplateCellEntity> cells) {

		if (userId == null || templateId == null || cells == null) {
			throw new IllegalArgumentException(
					"ユーザーID・テンプレートID・セル情報は必須です。");
		}

		if ((workId == null) == (articleId == null)) {
			throw new IllegalArgumentException(
					"下書きIDまたは記事IDのどちらか一方が必要です。");
		}

		if (workId != null) {

			// 下書きに紐づくテンプレート設定を保存する
			articleTemplateService.saveWorkTemplate(
					userId,
					workId,
					templateId,
					backgroundImageAssetId,
					backgroundColor);

			// 下書きに紐づくセル情報を置き換える
			articleTemplateCellService.saveWorkCells(
					userId,
					workId,
					templateId,
					cells);

		} else {

			// 公開記事に紐づくテンプレート設定を保存する
			articleTemplateService.saveArticleTemplate(
					userId,
					articleId,
					templateId,
					backgroundImageAssetId,
					backgroundColor);

			// 公開記事に紐づくセル情報を置き換える
			articleTemplateCellService.saveArticleCells(
					userId,
					articleId,
					templateId,
					cells);
		}
	}
}
