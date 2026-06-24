package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ArticleWorkspace;
import com.app.myblogpusher.repository.ArticleWorkspaceRepository;

@Service
public class ArticleWorkspaceService {

	@Autowired
	private ArticleWorkspaceRepository repository;

	public void save(
			Long userId,
			Long categoryId,
			String title,
			String content) {

		ArticleWorkspace workspace = repository.findById(userId)
				.orElse(new ArticleWorkspace());

		workspace.setUserId(userId);
		workspace.setCategoryId(categoryId);
		workspace.setTitle(title);
		workspace.setContent(content);
		workspace.setUpdateDate(LocalDateTime.now());

		repository.save(workspace);
	}

	public Optional<ArticleWorkspace> find(Long userId) {
		return repository.findById(userId);
	}

	public void delete(Long userId) {
		repository.deleteById(userId);
	}
}