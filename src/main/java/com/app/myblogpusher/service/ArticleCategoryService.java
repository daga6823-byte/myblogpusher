package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.CategoryDictionaryView;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.repository.ArticleCategoryRepository;
import com.app.myblogpusher.repository.TypoCorrectionRepository;

@Service
public class ArticleCategoryService {

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	public List<ArticleCategory> findByUserId(Long userId) {
		return articleCategoryRepository.findByUserId(userId);
	}

	public Optional<ArticleCategory> findByUserIdAndName(Long userId, String categoryName) {
		return articleCategoryRepository.findByUserIdAndCategoryName(userId, categoryName);
	}

	public Long insertCategory(
			Long userId,
			String categoryName,
			Long parentCategoryId,
			String displayName) {
		ArticleCategory newCategory = new ArticleCategory();
		newCategory.setUserId(userId);
		newCategory.setCategoryName(categoryName);
		newCategory.setCreateDate(LocalDateTime.now());
		newCategory.setUpdateDate(LocalDateTime.now());
		newCategory.setCreateUser(userId);
		newCategory.setUpdateUser(userId);
		newCategory.setParentCategoryId(parentCategoryId);
		newCategory.setDisplayName(displayName);
		articleCategoryRepository.save(newCategory);
		return newCategory.getCategoryId();
	}

	public Optional<ArticleCategory> findById(Long categoryId) {
		return articleCategoryRepository.findById(categoryId);
	}

	public List<CategoryDictionaryView> findDictionaryView(Long userId) {

		List<ArticleCategory> categories = articleCategoryRepository.findByUserId(userId);

		if (categories.isEmpty()) {
			return List.of();
		}

		List<Long> categoryIds = categories.stream()
				.map(ArticleCategory::getCategoryId)
				.toList();

		Map<Long, Long> countMap = typoCorrectionRepository.countByCategoryIds(categoryIds)
				.stream()
				.collect(Collectors.toMap(
						row -> (Long) row[0],
						row -> (Long) row[1]));
		
		Map<Long, String> categoryNameMap = categories.stream()
		        .collect(Collectors.toMap(
		                ArticleCategory::getCategoryId,
		                ArticleCategory::getDisplayName));

		return categories.stream()
				.map(c -> new CategoryDictionaryView(
						c.getCategoryId(),
						c.getCategoryName(),
						c.getParentCategoryId(),
						categoryNameMap.get(c.getParentCategoryId()),
						c.getDisplayName(),
						countMap.getOrDefault(c.getCategoryId(), 0L)))
				.toList();
	}

	public void updateCategory(Long categoryId,
			Long userId,
			String categoryName,
			Long parentCategoryId,
			String displayName) {

		ArticleCategory category = articleCategoryRepository.findById(categoryId).orElseThrow();

		if (!category.getUserId().equals(userId)) {
			throw new IllegalStateException("他のユーザーのカテゴリーは変更できません");
		}

		category.setCategoryName(categoryName);
		category.setParentCategoryId(parentCategoryId);
		category.setDisplayName(displayName);
		category.setUpdateUser(userId);
		category.setDisplayName(displayName);
		category.setParentCategoryId(parentCategoryId);
		category.setUpdateDate(LocalDateTime.now());

		articleCategoryRepository.save(category);
	}

	@Autowired
	private TypoCorrectionRepository typoCorrectionRepository;

	public void delete(Long categoryId, Long userId) {

		ArticleCategory category = articleCategoryRepository.findById(categoryId).orElseThrow();

		if (!category.getUserId().equals(userId)) {
			throw new IllegalStateException("他のユーザーのカテゴリーは削除できません");
		}

		long typoCount = typoCorrectionRepository.countByCategoryIds(List.of(categoryId))
				.stream()
				.findFirst()
				.map(row -> (Long) row[1])
				.orElse(0L);

		if (typoCount > 0) {
			throw new IllegalStateException("使用中のカテゴリーは削除できません");
		}

		articleCategoryRepository.delete(category);
	}

}