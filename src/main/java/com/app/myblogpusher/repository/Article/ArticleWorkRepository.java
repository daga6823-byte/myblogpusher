package com.app.myblogpusher.repository.Article;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.myblogpusher.entity.Article.ArticleWork;

public interface ArticleWorkRepository extends JpaRepository<ArticleWork, Long> {

	Optional<ArticleWork> findByWorkIdAndUserId(Long workId, Long userId);

	Optional<ArticleWork> findByArticleId(Long articleId);

	List<ArticleWork> findByUserIdOrderByUpdateDateDesc(Long userId);

	Optional<ArticleWork> findByUserIdAndCategoryGroupIdAndTitleAndContent(
			Long userId,
			Long categoryGroupId,
			String title,
			String content);

	Optional<ArticleWork> findByUserIdAndHugoPathAndTitleAndContent(
			Long userId,
			String hugoPath,
			String title,
			String content);

	Optional<ArticleWork> findBySlug(String slug);

	/**
	 * ユーザーの指定ステータスの記事を更新日時の降順で取得する
	 */
	List<ArticleWork> findByUserIdAndStatusOrderByUpdateDateDesc(
			Long userId,
			Integer status);

	/**
	 * 投稿前の記事だけを投稿中に更新する
	 *
	 * 更新件数が1件なら投稿開始可能、0件なら受付不可。
	 */
	@Modifying
	@Query("UPDATE ArticleWork w SET w.status = 1 "
			+ "WHERE w.workId = :workId "
			+ "AND w.userId = :userId "
			+ "AND w.status IN (0, 2)")
	int startPublishing(
			@Param("workId") Long workId,
			@Param("userId") Long userId);

}