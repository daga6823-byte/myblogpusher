package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.ArticleWorkspace;
import com.app.myblogpusher.repository.Article.ArticleWorkspaceRepository;

import jakarta.persistence.EntityManager;

@Service
public class ArticleWorkspaceService {

	@Autowired
	private ArticleWorkspaceRepository repository;

	@Autowired
	private EntityManager entityManager;

	public void save(Long userId, Long categoryGroupId, String hugoPath,
			String title, String content) {

		Optional<ArticleWorkspace> existingWorkspace = repository.findById(userId);

		ArticleWorkspace workspace;

		if (existingWorkspace.isPresent()) {
			workspace = existingWorkspace.get();
		} else {
			workspace = new ArticleWorkspace();
			workspace.setUserId(userId);
			entityManager.persist(workspace);
		}

		workspace.setCategoryGroupId(categoryGroupId);
		workspace.setHugoPath(hugoPath);
		workspace.setTitle(title);
		workspace.setContent(content);
		workspace.setUpdateDate(LocalDateTime.now());
	}

	public Optional<ArticleWorkspace> find(Long userId) {
		return repository.findById(userId);
	}

	public void delete(Long userId) {
		repository.deleteById(userId);
	}
}