package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.repository.ArticleWorkRepository;

@Service
public class ArticleWorkService {

	@Autowired
	private ArticleWorkRepository articleWorkRepository;

	public List<ArticleWork> findByUserId(Long userId) {
		return articleWorkRepository.findByUserId(userId);
	}

	public Long insertArticleWork(Long userId, Long categoryId, String title, String content, String slug) {
		ArticleWork work = new ArticleWork();
		work.setUserId(userId);
		work.setCategoryId(categoryId);
		work.setTitle(title);
		work.setContent(content);
		work.setSlug(slug);
		work.setCreateUser(userId);
		work.setUpdateUser(userId);
		work.setCreateDate(LocalDateTime.now());
		work.setUpdateDate(LocalDateTime.now());
		articleWorkRepository.save(work);
		return work.getWorkId();
	}

	public void updateArticleWork(Long workId, Long categoryId, String title, String content, Long userId, String slug) {
		ArticleWork work = articleWorkRepository.findById(workId).orElseThrow();
		work.setCategoryId(categoryId);
		work.setTitle(title);
		work.setContent(content);
		work.setSlug(slug);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());
		articleWorkRepository.save(work);
	}

	public ArticleWork findById(Long workId) {
		return articleWorkRepository.findById(workId).orElseThrow();
	}

	public Optional<ArticleWork> findDuplicate(Long userId, Long categoryId, String title, String content) {
		return articleWorkRepository.findByUserIdAndCategoryIdAndTitleAndContent(userId, categoryId, title, content);
	}

	public void delete(Long workId, Long userId) {
		ArticleWork work = articleWorkRepository.findById(workId).orElseThrow();

		if (!work.getUserId().equals(userId)) {
			throw new IllegalStateException("他のユーザーの記事は削除できません");
		}

		articleWorkRepository.delete(work);
	}
	
	public Optional<ArticleWork> findBySlug(String slug) {
		return articleWorkRepository.findBySlug(slug);
	}
	
}