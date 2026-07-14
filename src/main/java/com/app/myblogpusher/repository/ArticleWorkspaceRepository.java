package com.app.myblogpusher.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.ArticleWorkspace;

public interface ArticleWorkspaceRepository
		extends JpaRepository<ArticleWorkspace, Long> {
}