/**
 * 記事参考文献情報へのDBアクセスを担当するRepository
 *
 * カテゴリー単位で登録された参考文献の検索・保存・削除を行う。
 */

package com.app.myblogpusher.repository.Article;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.app.myblogpusher.entity.Article.ArticleReference;

public interface ArticleReferenceRepository
		extends JpaRepository<ArticleReference, Long> {

	/**
	 * ユーザー・カテゴリー単位で参考文献を取得する
	 */
	List<ArticleReference> findByUserIdAndCategoryIdOrderByReferenceNameAsc(
			Long userId,
			Long categoryId);

	/**
	 * カテゴリー単位で参考文献を取得する
	 */
	List<ArticleReference> findByCategoryIdOrderByReferenceNameAsc(
			Long categoryId);

	/**
	 * ユーザーが登録している参考文献を名称順で取得する
	 */
	List<ArticleReference> findByUserIdOrderByReferenceNameAsc(
			Long userId);

	/**
	 * ユーザーが参考文献を登録しているカテゴリーID一覧を取得する
	 */
	@Query("""
			SELECT DISTINCT ar.categoryId
			FROM ArticleReference ar
			WHERE ar.userId = :userId
			ORDER BY ar.categoryId
			""")
	List<Long> findDistinctCategoryIdByUserId(Long userId);
}