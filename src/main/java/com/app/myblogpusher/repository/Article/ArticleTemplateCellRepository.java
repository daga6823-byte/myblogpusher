
package com.app.myblogpusher.repository.Article;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Article.ArticleTemplateCellEntity;

public interface ArticleTemplateCellRepository
		extends JpaRepository<ArticleTemplateCellEntity, Long> {

	/**
	 * 編集中の記事に紐づくセル情報を取得する
	 */
	List<ArticleTemplateCellEntity> findByUserIdAndWorkIdOrderByIdAsc(
			Long userId,
			Long workId);

	/**
	 * 公開済み記事に紐づくセル情報を取得する
	 */
	List<ArticleTemplateCellEntity> findByUserIdAndArticleIdOrderByIdAsc(
			Long userId,
			Long articleId);

	/**
	 * 編集中の記事の指定セル情報を取得する
	 */
	Optional<ArticleTemplateCellEntity> findByUserIdAndWorkIdAndCellId(
			Long userId,
			Long workId,
			Long cellId);

	/**
	 * 公開済み記事の指定セル情報を取得する
	 */
	Optional<ArticleTemplateCellEntity> findByUserIdAndArticleIdAndCellId(
			Long userId,
			Long articleId,
			Long cellId);

	/**
	 * 編集中の記事のセル情報を一括削除する
	 */
	void deleteByUserIdAndWorkId(Long userId, Long workId);

	/**
	 * 公開済み記事のセル情報を一括削除する
	 */
	void deleteByUserIdAndArticleId(Long userId, Long articleId);
}
