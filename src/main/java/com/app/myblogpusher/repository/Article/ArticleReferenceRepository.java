/**
 * 記事参考文献情報へのDBアクセスを担当するRepository
 *
 * カテゴリー経路単位で登録された参考文献の検索・保存・削除を行う。
 */

package com.app.myblogpusher.repository.Article;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Article.ArticleReference;

public interface ArticleReferenceRepository extends JpaRepository<ArticleReference, Long> {

	/**
	 * ユーザー・カテゴリー経路単位で参考文献を取得する
	 */
	List<ArticleReference> findByUserIdAndGroupIdOrderByReferenceNameAsc(
			Long userId,
			Long groupId);

	/**
	 * 同一カテゴリー経路内の参考文献検索
	 */
	List<ArticleReference> findByGroupIdOrderByReferenceNameAsc(
			Long groupId);
	
	List<ArticleReference> findByUserIdOrderByReferenceNameAsc(
			Long userId);
}