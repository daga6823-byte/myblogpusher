package com.app.myblogpusher.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.app.myblogpusher.entity.Article.ArticleEntity;
import com.app.myblogpusher.entity.Article.ArticleWorkEntity;
import com.app.myblogpusher.enums.ArticleStatus;

@Component
public class ArticleMapper {

	/**
	 * 下書きから投稿済み記事を生成する
	 */
	public ArticleEntity toArticle(ArticleWorkEntity work) {

		ArticleEntity article = new ArticleEntity();

		copyWorkToArticle(work, article);

		LocalDateTime now = LocalDateTime.now();

		article.setCreateDate(now);
		article.setPublishDate(now);

		return article;
	}

	/**
	 * 下書き内容を投稿済み記事へ反映する
	 */
	public void copyWorkToArticle(ArticleWorkEntity work, ArticleEntity article) {

		article.setUserId(work.getUserId());

		article.setTitle(work.getTitle());
		article.setSlug(work.getSlug());
		article.setContent(work.getContent());

		article.setCreateUser(work.getCreateUser());
		article.setUpdateUser(work.getUpdateUser());

		article.setStatus(ArticleStatus.PUBLISHING);
	}

}