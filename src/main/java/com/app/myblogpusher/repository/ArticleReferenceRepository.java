/**
 * 記事参考文献情報へのDBアクセスを担当するRepository
 *
 * カテゴリー単位で登録された参考文献の検索・保存・削除を行う。
 */

package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.myblogpusher.entity.ArticleReference;

@Repository
public interface ArticleReferenceRepository extends JpaRepository<ArticleReference, Long> {

	/**
	 * ユーザー・カテゴリー単位で参考文献を取得する
	 */
	List<ArticleReference> findByUserIdAndCategoryIdOrderByReferenceNameAsc(
			Long userId,
			Long categoryId);

	/**
	 * 同一カテゴリー内の参考文献検索
	 */
	List<ArticleReference> findByCategoryIdOrderByReferenceNameAsc(
			Long categoryId);
}