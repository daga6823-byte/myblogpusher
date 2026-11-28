package com.app.myblogpusher.repository.Article;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.myblogpusher.entity.Article.ArticleEntity;
import com.app.myblogpusher.enums.ArticleStatus;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {

	/**
	 * ユーザーの投稿済み記事一覧を取得
	 */
	List<ArticleEntity> findByUserIdOrderByUpdateDateDesc(Long userId);

	/**
	 * ユーザーとslugで記事を取得
	 *
	 * 同一slugの記事が複数存在する場合もあるため、
	 * 投稿処理などで重複を確認する場合はListで取得する。
	 */
	Optional<ArticleEntity> findByUserIdAndSlug(
			Long userId,
			String slug);

	/**
	 * ユーザーとslugで記事を複数取得
	 *
	 * 同一slugの記事が複数存在する場合の重複確認用。
	 */
	List<ArticleEntity> findAllByUserIdAndSlug(
			Long userId,
			String slug);

	/**
	 * ユーザーとHugoパスで記事を取得
	 */
	Optional<ArticleEntity> findByUserIdAndHugoPath(
			Long userId,
			String hugoPath);

	/**
	 * ユーザーの公開済み記事一覧を取得
	 */
	List<ArticleEntity> findByUserIdAndStatusOrderByUpdateDateDesc(
			Long userId,
			ArticleStatus status);

	void deleteByUserIdAndSlug(
			Long userId,
			String slug);

	List<ArticleEntity> findByUserId(Long userId);

	/**
	 * ユーザーIDと記事IDで記事を取得する
	 *
	 * 記事テンプレート設定の保存時に所有者を確認する。
	 */
	Optional<ArticleEntity> findByUserIdAndArticleId(
			Long userId,
			Long articleId);

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
			FROM ArticleEntity a
			WHERE a.userId = :userId
			AND a.hugoPath LIKE CONCAT(:hugoPath, '%')
			""")
	List<ArticleEntity> findLinkArticles(
			@Param("userId") Long userId,
			@Param("hugoPath") String hugoPath);
}