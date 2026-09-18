package com.app.myblogpusher.repository.Article;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.enums.ArticleStatus;

public interface ArticleRepository extends JpaRepository<Article, Long> {

	/**
	 * ユーザーの投稿済み記事一覧を取得
	 */
	List<Article> findByUserIdOrderByUpdateDateDesc(Long userId);

	/**
	 * ユーザーとslugで記事を取得
	 *
	 * 同一slugの記事が複数存在する場合もあるため、
	 * 投稿処理などで重複を確認する場合はListで取得する。
	 */
	Optional<Article> findByUserIdAndSlug(
			Long userId,
			String slug);

	/**
	 * ユーザーとslugで記事を複数取得
	 *
	 * 同一slugの記事が複数存在する場合の重複確認用。
	 */
	List<Article> findAllByUserIdAndSlug(
			Long userId,
			String slug);

	/**
	 * ユーザーとHugoパスで記事を取得
	 */
	Optional<Article> findByUserIdAndHugoPath(
			Long userId,
			String hugoPath);

	/**
	 * ユーザーの公開済み記事一覧を取得
	 */
	List<Article> findByUserIdAndStatusOrderByUpdateDateDesc(
			Long userId,
			ArticleStatus status);

	void deleteByUserIdAndSlug(
			Long userId,
			String slug);

	List<Article> findByUserId(Long userId);

	/**
	 * 記事リンク挿入用
	 *
	 * 指定したHugoパス配下の記事を取得する。
	 *
	 * 例:
	 * movie/batman
	 * ↓
	 * movie/batman/*
	 */
	@Query("""
			SELECT a
			FROM Article a
			WHERE a.userId = :userId
			AND a.hugoPath LIKE CONCAT(:hugoPath, '%')
			""")
	List<Article> findLinkArticles(
			@Param("userId") Long userId,
			@Param("hugoPath") String hugoPath);

	/**
	 * 記事リンク挿入用
	 *
	 * 指定したカテゴリーの記事を取得する。
	 */
	List<Article> findByUserIdAndCategoryGroupIdOrderByUpdateDateDesc(
			Long userId,
			Long categoryGroupId);
}