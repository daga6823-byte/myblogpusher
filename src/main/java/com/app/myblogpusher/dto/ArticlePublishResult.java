/*
新規/更新判定を保持するDTO
*/

package com.app.myblogpusher.dto;

import com.app.myblogpusher.entity.Article;

public class ArticlePublishResult {

	private Article article;
	private boolean newArticle;

	public ArticlePublishResult(
			Article article,
			boolean newArticle) {

		this.article = article;
		this.newArticle = newArticle;
	}

	public Article getArticle() {
		return article;
	}

	public boolean isNewArticle() {
		return newArticle;
	}
}