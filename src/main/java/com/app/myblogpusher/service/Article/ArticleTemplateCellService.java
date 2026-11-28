
/**
 * 記事ごとのテンプレートセル情報を管理するサービス
 *
 * 編集中の記事のセル情報の取得・保存と、
 * 公開時にArticleWorkからArticleへの引き継ぎを担当する。
 *
 * セルの配置や装飾などのマスター情報は変更しない。
 * セル情報はユーザー単位で管理する。
 */
package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Article.ArticleTemplateCellEntity;
import com.app.myblogpusher.repository.Article.ArticleTemplateCellRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleTemplateCellService {

	private final ArticleTemplateCellRepository articleTemplateCellRepository;

	private final ArticleWorkService articleWorkService;

	/**
	 * 編集中の記事のセル情報を取得する
	 */
	@Transactional(readOnly = true)
	public List<ArticleTemplateCellEntity> findByWorkId(
			Long userId,
			Long workId) {

		return articleTemplateCellRepository
				.findByUserIdAndWorkIdOrderByIdAsc(userId, workId);
	}

	/**
	 * 編集中の記事のセル情報を置き換えて保存する
	 *
	 * VARIABLE値・IMAGE割り当て・背景画像IDを保存する。
	 */
	@Transactional
	public void saveWorkCells(Long userId, Long workId, Long templateId,
			List<ArticleTemplateCellEntity> cells) {
		validateSaveParameters(userId, templateId, cells);
		articleWorkService.findById(workId, userId);

		LocalDateTime now = LocalDateTime.now();

		for (ArticleTemplateCellEntity cell : cells) {
			prepareCell(cell, userId, workId, null, templateId, now);
		}

		articleTemplateCellRepository.deleteByUserIdAndWorkId(userId, workId);

		if (!cells.isEmpty()) {
			articleTemplateCellRepository.saveAll(cells);
		}
	}

	/**
	 * 公開済み記事のセル情報を取得する
	 */
	@Transactional(readOnly = true)
	public List<ArticleTemplateCellEntity> findByArticleId(
			Long userId,
			Long articleId) {

		return articleTemplateCellRepository
				.findByUserIdAndArticleIdOrderByIdAsc(userId, articleId);
	}

	/**
	 * 公開済み記事のセル情報を置き換えて保存する
	 *
	 * VARIABLE値・IMAGE割り当て・背景画像IDを保存する。
	 */
	@Transactional
	public void saveArticleCells(Long userId, Long articleId, Long templateId,
	        List<ArticleTemplateCellEntity> cells) {
	    validateSaveParameters(userId, templateId, cells);

	    if (articleId == null) {
	        throw new IllegalArgumentException("記事IDは必須です。");
	    }

	    LocalDateTime now = LocalDateTime.now();

	    for (ArticleTemplateCellEntity cell : cells) {
	        prepareCell(cell, userId, null, articleId, templateId, now);
	    }

	    articleTemplateCellRepository.deleteByUserIdAndArticleId(userId, articleId);

	    if (!cells.isEmpty()) {
	        articleTemplateCellRepository.saveAll(cells);
	    }
	}

	/**
	 * 保存パラメーターを検証する
	 */
	private void validateSaveParameters(
			Long userId,
			Long templateId,
			List<ArticleTemplateCellEntity> cells) {

		if (userId == null) {
			throw new IllegalArgumentException("ユーザーIDは必須です。");
		}

		if (templateId == null) {
			throw new IllegalArgumentException("テンプレートIDは必須です。");
		}

		if (cells == null) {
			throw new IllegalArgumentException("セル情報が指定されていません。");
		}

		for (ArticleTemplateCellEntity cell : cells) {

			if (cell == null) {
				throw new IllegalArgumentException(
						"セル情報にnullが含まれています。");
			}

			if (cell.getCellId() == null) {
				throw new IllegalArgumentException(
						"セルIDが指定されていません。");
			}
		}
	}

	/**
	 * 保存するセル情報に所有者・記事・テンプレート情報を設定する
	 */
	private void prepareCell(ArticleTemplateCellEntity cell, Long userId, Long workId, Long articleId,
	        Long templateId, LocalDateTime now) {
	    cell.setId(null);
	    cell.setUserId(userId);
	    cell.setWorkId(workId);
	    cell.setArticleId(articleId);
	    cell.setTemplateId(templateId);
	    cell.setCreateDate(now);
	    cell.setUpdateDate(now);
	}

	/**
	 * 公開時に編集用セル情報を記事側へ引き継ぐ
	 *
	 * 既存記事のセル情報を置き換えたうえで、
	 * work_idからarticle_idへ所有先を変更する。
	 *
	 * 背景画像IDも各セルレコードに保持したまま引き継ぐ。
	 */
	@Transactional
	public void transferWorkCellsToArticle(
			Long userId,
			Long workId,
			Long articleId) {

		if (userId == null || workId == null || articleId == null) {
			throw new IllegalArgumentException(
					"ユーザーID・作業ID・記事IDは必須です。");
		}

		List<ArticleTemplateCellEntity> cells = articleTemplateCellRepository
				.findByUserIdAndWorkIdOrderByIdAsc(userId, workId);

		// 既存記事の古いセル情報を削除する。
		articleTemplateCellRepository
				.deleteByUserIdAndArticleId(userId, articleId);

		LocalDateTime now = LocalDateTime.now();

		for (ArticleTemplateCellEntity cell : cells) {
			cell.setWorkId(null);
			cell.setArticleId(articleId);
			cell.setUpdateDate(now);
		}

		// work_idを解除し、article_idへ引き継ぐ。
		if (!cells.isEmpty()) {
			articleTemplateCellRepository.saveAll(cells);
		}
	}

	/**
	 * 編集中の記事のセル情報を削除する
	 */
	@Transactional
	public void deleteWorkCells(Long userId, Long workId) {
		articleTemplateCellRepository
				.deleteByUserIdAndWorkId(userId, workId);
	}
}
