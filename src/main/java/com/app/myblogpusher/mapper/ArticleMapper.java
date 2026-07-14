package com.app.myblogpusher.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.enums.ArticleStatus;

@Component
public class ArticleMapper {

	/**
	 * 下書きから投稿済み記事を生成する
	 */
	public Article toArticle(ArticleWork work) {

		Article article = new Article();

		copyWorkToArticle(work, article);

		LocalDateTime now = LocalDateTime.now();

		article.setCreateDate(now);
		article.setPublishDate(now);

		return article;
	}

	/**
	 * 下書き内容を投稿済み記事へ反映する
	 */
	public void copyWorkToArticle(ArticleWork work, Article article) {

		article.setUserId(work.getUserId());
		article.setCategoryId(work.getCategoryId());

		article.setTitle(work.getTitle());
		article.setSlug(work.getSlug());
		article.setContent(work.getContent());

		article.setCreateUser(work.getCreateUser());
		article.setUpdateUser(work.getUpdateUser());

		article.setUpdateDate(LocalDateTime.now());

		article.setStatus(ArticleStatus.PUBLISHING);
	}

}