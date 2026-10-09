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
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Article.ArticleFormatService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Category.CategoryPathService;
import com.app.myblogpusher.service.Category.CategoryRelationService;

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
	private CategoryPathService categoryPathService;

	@Autowired
	private CategoryRelationService categoryRelationService;

	public Long doSaveDraft(
			Long workId,
			String categorySelect,
			String newCategoryName,
			String newCategoryDisplayName,
			String title,
			String content,
			Long userId) {

		CategoryResolution categoryResolution = resolveCategory(
				userId,
				categorySelect,
				newCategoryName,
				newCategoryDisplayName);

		Long categoryGroupId = categoryResolution.groupId();

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

			Long savedWorkId = articleWorkService.insertArticleWork(
					userId,
					null,
					categoryGroupId,
					title,
					formattedContent,
					slug);

			if (categoryResolution.groupIdPending()) {
				categoryRelationService.updateArticleWorkCategoryGroupId(
						savedWorkId,
						categoryResolution.categoryId());
			}

			return savedWorkId;

		} else {

			articleWorkService.updateArticleWork(
					workId,
					categoryGroupId,
					title,
					formattedContent,
					userId,
					slug);

			if (categoryResolution.groupIdPending()) {
				categoryRelationService.updateArticleWorkCategoryGroupId(
						workId,
						categoryResolution.categoryId());
			}

			return workId;
		}
	}

	/**
	 * カテゴリー選択値を解決する。
	 *
	 * 新規カテゴリーの場合はCategoryRelationの登録が非同期になるため、
	 * groupIdがまだ存在しない場合はArticleWorkを先に保存できるようにする。
	 */
	private CategoryResolution resolveCategory(
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

			// 新カテゴリー作成後は、現在の選択カテゴリーに
			// 新カテゴリー名を追加した完全フルパスからgroupIdを取得する。
			String newCategoryPath = categorySelect == null
					|| categorySelect.isBlank()
							? newCategoryName
							: categorySelect + "/" + newCategoryName;

			Long groupId = categoryPathService.findGroupIdByFullPath(
					userId,
					newCategoryPath);

			// CategoryRelationは非同期登録のため、
			// まだgroupIdが採番されていない場合はArticleWorkを先に保存する。
			if (groupId == null) {
				return new CategoryResolution(
						null,
						categoryId,
						true);
			}

			return new CategoryResolution(
					groupId,
					categoryId,
					false);
		}

		if (categorySelect == null || categorySelect.isBlank()) {
			return new CategoryResolution(
					null,
					null,
					false);
		}

		Long groupId = categoryPathService.findGroupIdByFullPath(
				userId,
				categorySelect);

		if (groupId == null) {

			// 欠落しているカテゴリー経路を同期的に補完する。
			categoryRelationService.repairCategoryPath(
					userId,
					categorySelect);

			// 補完後にgroupIdを再取得する。
			groupId = categoryPathService.findGroupIdByFullPath(
					userId,
					categorySelect);
		}

		if (groupId == null) {
			throw new IllegalStateException(
					"カテゴリー経路を復旧できませんでした: " + categorySelect);
		}

		return new CategoryResolution(
				groupId,
				null,
				false);
	}

	/**
	 * カテゴリー解決結果を保持する。
	 *
	 * groupIdがまだ採番されていない場合は、
	 * ArticleWork保存後に非同期でgroupIdを反映する。
	 */
	private record CategoryResolution(
			Long groupId,
			Long categoryId,
			boolean groupIdPending) {
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
