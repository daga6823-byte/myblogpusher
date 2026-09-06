package com.app.myblogpusher.repository.Article;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Article.ArticleWork;

public interface ArticleWorkRepository extends JpaRepository<ArticleWork, Long> {
	List<ArticleWork> findByUserIdOrderByUpdateDateDesc(Long userId);

	Optional<ArticleWork> findByUserIdAndCategoryGroupIdAndTitleAndContent(
			Long userId,
			Long categoryGroupId,
			String title,
			String content);

	Optional<ArticleWork> findBySlug(String slug);

	/**
	 * ユーザーの指定ステータスの記事を更新日時の降順で取得する
	 */
	List<ArticleWork> findByUserIdAndStatusOrderByUpdateDateDesc(
			Long userId,
			Integer status);

}