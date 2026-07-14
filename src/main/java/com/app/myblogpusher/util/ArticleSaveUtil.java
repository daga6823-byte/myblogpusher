/**
 * 記事の下書き保存処理を担当するユーティリティ
 * カテゴリー選択値(categorySelect)の解決、本文フォーマット、スラッグ生成、
 * ArticleWorkの新規作成/更新/重複チェックを行う
 */

package com.app.myblogpusher.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.repository.ArticleRepository;
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

	@Autowired
	private ArticleRepository articleRepository;
	
	public Long doSaveDraft(Long workId, String categorySelect, String newCategoryName,
			String title, String content, Long userId) {

		Long categoryId = resolveCategoryId(userId, categorySelect, newCategoryName);

		String formattedContent = articleFormatService.formatContent(content);
		String slug = slugUtil.generateSlug(title);

		if (workId == null) {
			if ((title == null || title.isBlank()) && (formattedContent == null || formattedContent.isBlank())) {
				return null;
			}
			Optional<ArticleWork> existing = articleWorkService.findDuplicate(userId, categoryId, title,
					formattedContent);
			if (existing.isPresent()) {
				return existing.get().getWorkId();
			}
			return articleWorkService.insertArticleWork(
					userId,
					categoryId,
					title,
					formattedContent,
					slug);
		} else {
			articleWorkService.updateArticleWork(
					workId,
					categoryId,
					title,
					formattedContent,
					userId,
					slug);
			return workId;
		}
	}

	/**
	 * categorySelectを解釈してcategoryIdを返す。
	 * "__new__"の場合は新規ルートカテゴリー（parentCategoryId=null）として作成する。
	 * それ以外は選択プルダウンから渡されたcategoryIdの文字列をそのまま数値変換する。
	 */
	private Long resolveCategoryId(Long userId, String categorySelect, String newCategoryName) {
		if ("__new__".equals(categorySelect)) {
			return articleCategoryService.findByUserIdAndName(userId, newCategoryName)
					.map(ArticleCategory::getCategoryId)
					.orElseGet(() -> articleCategoryService.insertCategory(
							userId,
							newCategoryName,
							null,
							newCategoryName));
		}
		return Long.parseLong(categorySelect);
	}
	
	@Transactional
	public void deleteByUserIdAndSlug(
			Long userId,
			String slug) {

		articleRepository.deleteByUserIdAndSlug(
				userId,
				slug);
	}
}