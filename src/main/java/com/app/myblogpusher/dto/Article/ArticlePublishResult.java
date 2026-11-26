/*
新規/更新判定を保持するDTO
*/

package com.app.myblogpusher.dto.Article;

import com.app.myblogpusher.entity.Article.ArticleEntity;

public class ArticlePublishResult {

	private ArticleEntity article;
	private boolean newArticle;

	public ArticlePublishResult(
			ArticleEntity article,
			boolean newArticle) {

		this.article = article;
		this.newArticle = newArticle;
	}

	public ArticleEntity getArticle() {
		return article;
	}

	public boolean isNewArticle() {
		return newArticle;
	}
}