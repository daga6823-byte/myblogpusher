package com.app.myblogpusher.service.Article;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Article.ArticleWorkspaceEntity;
import com.app.myblogpusher.repository.Article.ArticleWorkspaceRepository;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class ArticleWorkspaceService {

	@Autowired
	private ArticleWorkspaceRepository repository;

	@Autowired
	private EntityManager entityManager;

	public void save(Long userId, Long categoryGroupId, String hugoPath,
			String title, String content) {

		Optional<ArticleWorkspaceEntity> existingWorkspace = repository.findById(userId);

		ArticleWorkspaceEntity workspace;

		if (existingWorkspace.isPresent()) {
			workspace = existingWorkspace.get();
		} else {
			workspace = new ArticleWorkspaceEntity();
			workspace.setUserId(userId);
		}

		workspace.setCategoryGroupId(categoryGroupId);
		workspace.setHugoPath(hugoPath);
		workspace.setTitle(title);
		workspace.setContent(content);

		if (existingWorkspace.isEmpty()) {
			entityManager.persist(workspace);
		}
	}

	public Optional<ArticleWorkspaceEntity> find(Long userId) {
		return repository.findById(userId);
	}

	public void delete(Long userId) {
		repository.deleteById(userId);
	}
}