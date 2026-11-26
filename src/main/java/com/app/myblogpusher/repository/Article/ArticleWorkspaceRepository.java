package com.app.myblogpusher.repository.Article;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Article.ArticleWorkspaceEntity;

public interface ArticleWorkspaceRepository
		extends JpaRepository<ArticleWorkspaceEntity, Long> {
}