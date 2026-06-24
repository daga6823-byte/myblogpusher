package com.app.myblogpusher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.myblogpusher.entity.ArticleWorkspace;

@Repository
public interface ArticleWorkspaceRepository
		extends JpaRepository<ArticleWorkspace, Long> {
}