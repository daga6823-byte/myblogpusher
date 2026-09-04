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

import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Article.ArticleFormatService;
import com.app.myblogpusher.service.Article.ArticleWorkService;

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

	public Long doSaveDraft(
			Long workId,
			String categorySelect,
			String newCategoryName,
			String title,
			String content,
			Long userId) {

		Long categoryGroupId = resolveCategoryGroupId(
				userId,
				categorySelect,
				newCategoryName);

		String formattedContent = articleFormatService.formatContent(content);

		formattedContent = MarkdownFootnoteUtil.normalize(formattedContent);

		String slug = slugUtil.generateSlug(title);

		if (workId == null) {

			if ((title == null || title.isBlank())
					&& (formattedContent == null || formattedContent.isBlank())) {
				return null;
			}

			Optional<ArticleWork> existing = articleWorkService.findDuplicate(
					userId,
					categoryGroupId,
					title,
					formattedContent);

			if (existing.isPresent()) {
				return existing.get().getWorkId();
			}

			return articleWorkService.insertArticleWork(
					userId,
					null,
					categoryGroupId,
					title,
					formattedContent,
					slug);

		} else {

			articleWorkService.updateArticleWork(
					workId,
					categoryGroupId,
					title,
					formattedContent,
					userId,
					slug);

			return workId;
		}
	}

	/**
	 * categorySelectを解釈してcategoryGroupIdを返す。
	 *
	 * 既存カテゴリーの場合は、プルダウンから渡されたgroupIdを使用する。
	 * "__new__"の場合は新規カテゴリーを作成し、そのcategoryIdを
	 * 下書き段階の暫定的なcategoryGroupIdとして使用する。
	 */
	private Long resolveCategoryGroupId(
			Long userId,
			String categorySelect,
			String newCategoryName) {

		System.out.println("categorySelect = " + categorySelect);
		System.out.println("newCategoryName = " + newCategoryName);

		if ("__new__".equals(categorySelect)) {

			return articleCategoryService
					.findByUserIdAndName(userId, newCategoryName)
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
