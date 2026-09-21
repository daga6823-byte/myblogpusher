/**
 * 記事の下書き保存処理を担当するユーティリティ
 * カテゴリー選択値(categorySelect)の解決、本文フォーマット、スラッグ生成、
 * ArticleWorkの新規作成/更新/重複チェックを行う
 */

package com.app.myblogpusher.util;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Article.ArticleFormatService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Category.CategoryPathService;

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

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	@Autowired
	private CategoryPathService categoryPathService;

	public Long doSaveDraft(
			Long workId,
			String categorySelect,
			String newCategoryName,
			String newCategoryDisplayName,
			String title,
			String content,
			Long userId) {

		Long categoryGroupId = resolveCategoryGroupId(
				userId,
				categorySelect,
				newCategoryName,
				newCategoryDisplayName);

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
	 * 既存カテゴリーの場合は、画面から渡されたcategoryPathから
	 * CategoryRelationを検索してgroupIdを取得する。
	 * "__new__"の場合は新規カテゴリーを作成する。
	 */
	private Long resolveCategoryGroupId(
			Long userId,
			String categorySelect,
			String newCategoryName,
			String newCategoryDisplayName) {

		if (newCategoryName != null && !newCategoryName.isBlank()) {

			// categorySelectには、現在選択されている親カテゴリーまでの
			// フルパスが入っている。
			Long parentCategoryId = categoryPathService.findCategoryIdByFullPath(
					userId,
					categorySelect);

			List<Long> parentCategoryIds = parentCategoryId != null
					? List.of(parentCategoryId)
					: List.of();

			Long categoryId = articleCategoryService
					.findByUserIdAndName(userId, newCategoryName)
					.map(ArticleCategory::getCategoryId)
					.orElseGet(() -> articleCategoryService.insertCategory(
							userId,
							newCategoryName,
							parentCategoryIds,
							newCategoryDisplayName));

			// 新カテゴリーを作成した場合も、記事にはカテゴリーそのもののIDではなく
			// CategoryRelation.groupIdを設定する。
			return categoryRelationRepository
					.findByCategoryId(categoryId)
					.stream()
					.findFirst()
					.map(relation -> relation.getGroupId())
					.orElseThrow();
		}

		if (categorySelect == null || categorySelect.isBlank()) {
			return null;
		}

		return categoryRelationRepository
				.findByCategoryPath(categorySelect)
				.stream()
				.findFirst()
				.map(relation -> relation.getGroupId())
				.orElseThrow();
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
