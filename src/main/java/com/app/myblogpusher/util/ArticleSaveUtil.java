package com.app.myblogpusher.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleFormatService;
import com.app.myblogpusher.service.ArticleWorkService;

@Component
public class ArticleSaveUtil {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleFormatService articleFormatService;

	@Autowired
	private SlugUtil slugUtil;

	public Long doSaveDraft(Long workId, String categorySelect, String newCategoryName,
			String title, String content, Long userId) {

		String categoryName = "__new__".equals(categorySelect) ? newCategoryName : categorySelect;
		Long categoryId = articleCategoryService.findByUserIdAndName(userId, categoryName)
				.map(ArticleCategory::getCategoryId)
				.orElseGet(() -> articleCategoryService.insertCategory(userId, categoryName));

		String formattedContent = articleFormatService.formatContent(content);

		String slug = slugUtil.generateSlug(title);

		if (workId == null) {
			if ((title == null || title.isBlank()) && (formattedContent == null || formattedContent.isBlank())) {
				return null;
			}

			Optional<ArticleWork> existing = articleWorkService.findDuplicate(userId, categoryId, title, formattedContent);
			if (existing.isPresent()) {
				return existing.get().getWorkId();
			}

			return articleWorkService.insertArticleWork(userId, categoryId, title, content, slug);
		} else {
			articleWorkService.updateArticleWork(workId, categoryId, title, content, userId, slug);
			return workId;
		}
	}
}