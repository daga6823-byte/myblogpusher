/**
 * 投稿済み記事の登録・更新を担当するサービス
 *
 * ArticleエンティティのCRUDのみを担当する。
 * 投稿フローの制御はArticlePublishServiceが行う。
 */
package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.enums.ArticleStatus;
import com.app.myblogpusher.repository.ArticleRepository;

@Service
public class ArticleService {

	@Autowired
	private ArticleRepository articleRepository;

	public Article createFromWork(ArticleWork work) {

		Article article = new Article();

		article.setUserId(work.getUserId());
		article.setCategoryId(work.getCategoryId());
		article.setTitle(work.getTitle());
		article.setSlug(work.getSlug());
		article.setContent(work.getContent());

		article.setStatus(ArticleStatus.PUBLISHING);

		LocalDateTime now = LocalDateTime.now();

		article.setCreateDate(now);
		article.setUpdateDate(now);
		article.setPublishDate(now);

		article.setCreateUser(work.getCreateUser());
		article.setUpdateUser(work.getUpdateUser());

		return articleRepository.save(article);
	}

	/**
	 * 投稿済み記事を更新
	 */
	public Article updateFromWork(Article article, ArticleWork work) {

		article.setCategoryId(work.getCategoryId());
		article.setTitle(work.getTitle());
		article.setSlug(work.getSlug());
		article.setContent(work.getContent());

		article.setUpdateDate(LocalDateTime.now());
		article.setUpdateUser(work.getUpdateUser());

		article.setStatus(ArticleStatus.PUBLISHING);

		return articleRepository.save(article);
	}

	/**
	 * GitHub同期用
	 * 既存記事なら更新、存在しなければ新規登録
	 */
	public Article saveFromGitHub(
			Long userId,
			String slug,
			String title,
			String content,
			LocalDateTime publishDate) {

		System.out.println("保存：" + slug);

		Optional<Article> existing = articleRepository.findByUserIdAndSlug(userId, slug);

		Article article = existing.orElseGet(Article::new);

		LocalDateTime now = LocalDateTime.now();

		if (article.getArticleId() == null) {
			article.setUserId(userId);
			article.setCreateDate(now);
			article.setCreateUser(userId);
		}

		article.setTitle(title);
		article.setSlug(slug);
		article.setContent(content);
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setPublishDate(publishDate);
		article.setUpdateDate(now);
		article.setUpdateUser(userId);

		return articleRepository.save(article);
	}

	public void updateStatus(Long articleId, ArticleStatus status) {

		Article article = articleRepository.findById(articleId)
				.orElseThrow();

		article.setStatus(status);

		articleRepository.save(article);
	}

	/**
	 * 投稿済み記事IDから記事を取得する
	 */
	public Article findById(Long articleId) {

		return articleRepository.findById(articleId)
				.orElseThrow();

	}

	/**
	 * 公開済み記事一覧取得
	 */
	public List<Article> findPublishedByUserId(Long userId) {

		return articleRepository.findByUserIdAndStatusOrderByUpdateDateDesc(
				userId,
				ArticleStatus.PUBLISHED);
	}

	public Article findBySlug(Long userId, String slug) {

		return articleRepository.findByUserIdAndSlug(userId, slug)
				.orElse(null);
	}

}