package com.app.myblogpusher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.enums.ArticleStatus;

public interface ArticleRepository extends JpaRepository<Article, Long> {

	/**
	 * ユーザーの投稿済み記事一覧を取得
	 */
	List<Article> findByUserIdOrderByUpdateDateDesc(Long userId);

	/**
	 * ユーザーとslugで記事を取得
	 */
	Optional<Article> findByUserIdAndSlug(
			Long userId,
			String slug);

	/**
	 * ユーザーの公開済み記事一覧を取得
	 */
	List<Article> findByUserIdAndStatusOrderByUpdateDateDesc(
			Long userId,
			ArticleStatus status);

}