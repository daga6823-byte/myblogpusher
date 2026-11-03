/**
 * 記事の下書き保存処理を担当するユーティリティ
 *
 * カテゴリー選択値(categorySelect)の解決、本文フォーマット、
 * ArticleWorkの新規作成・更新・重複チェックを行う。
 * スラッグの生成は投稿確定時に行う。
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

		// categorySelectに新規カテゴリー名が含まれていない場合だけ追加する。
		String hugoPath = categorySelect;

		if (newCategoryName != null && !newCategoryName.isBlank()) {

			String lastCategoryName = hugoPath == null || hugoPath.isBlank()
					? null
					: hugoPath.substring(hugoPath.lastIndexOf("/") + 1);

			if (!newCategoryName.equals(lastCategoryName)) {

				hugoPath = hugoPath == null || hugoPath.isBlank()
						? newCategoryName
						: hugoPath + "/" + newCategoryName;
			}
		}

		// 添削・誤字チェック用の第2階層カテゴリーIDを取得する。
		Long typoCategoryId = null;

		if (hugoPath != null && !hugoPath.isBlank()) {

			String[] pathParts = hugoPath.split("/");

			if (pathParts.length >= 2) {

				String typoCategoryPath = pathParts[0] + "/" + pathParts[1];

				// 新規カテゴリー自体が第2階層の場合は、
				// 非同期のCategoryRelation登録を待たずに取得できるIDを使用する。
				if (newCategoryName != null
						&& !newCategoryName.isBlank()
						&& typoCategoryPath.equals(hugoPath)) {

					typoCategoryId = categoryResolution.categoryId();

				} else {

					typoCategoryId = categoryPathService.findCategoryIdByFullPath(
							userId,
							typoCategoryPath);
				}
			}
		}

		String formattedContent = articleFormatService.formatContent(content);
		formattedContent = MarkdownFootnoteUtil.normalize(formattedContent);

		String slug = null;

		if (workId != null) {
			ArticleWork work = articleWorkService.findById(workId, userId);

			if (work != null) {
				slug = work.getSlug();
			}
		}

		// 既存のslugがない場合は、タイトルからスラッグを生成する。
		// 形態素解析と英単語辞典による変換はSlugUtilに委譲する。
		if (slug == null || slug.isBlank()) {
			slug = slugUtil.generateSlug(title);
		}

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

			Long newWorkId = articleWorkService.insertArticleWork(
					userId,
					null,
					categoryGroupId,
					hugoPath,
					null,
					title,
					formattedContent,
					slug);

			if (categoryResolution.groupIdPending()) {
				categoryRelationService.updateArticleWorkCategoryGroupId(
						newWorkId,
						categoryResolution.categoryId(),
						hugoPath,
						userId);
			}

			return newWorkId;

		} else {

			typoCategoryId = articleWorkService.findById(workId, userId)
					.getTypoCategoryId();

			articleWorkService.updateArticleWork(
					workId,
					categoryGroupId,
					hugoPath,
					typoCategoryId,
					title,
					formattedContent,
					userId,
					slug);

			if (categoryResolution.groupIdPending()) {
				categoryRelationService.updateArticleWorkCategoryGroupId(
						workId,
						categoryResolution.categoryId(),
						hugoPath,
						userId);
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

			// categorySelectに新規カテゴリー名が含まれている場合は、
			// 末尾を除いたPathから親カテゴリーIDを取得する。
			String parentCategoryPath = categorySelect;

			if (parentCategoryPath != null
					&& !parentCategoryPath.isBlank()) {

				String lastCategoryName = parentCategoryPath.substring(
						parentCategoryPath.lastIndexOf("/") + 1);

				if (newCategoryName.equals(lastCategoryName)) {

					int lastSlash = parentCategoryPath.lastIndexOf("/");

					parentCategoryPath = lastSlash >= 0
							? parentCategoryPath.substring(0, lastSlash)
							: null;
				}
			}

			// 親カテゴリーの経路が存在する場合だけ、親カテゴリーIDを取得する。
			// 最上位カテゴリーの場合は親を持たないため、nullのまま登録する。
			Long parentCategoryId = parentCategoryPath == null
					|| parentCategoryPath.isBlank()
							? null
							: categoryPathService.findCategoryIdByFullPath(
									userId,
									parentCategoryPath);

			List<Long> parentCategoryIds = parentCategoryId != null
					? List.of(parentCategoryId)
					: List.of();

			Optional<ArticleCategory> existingCategory = articleCategoryService
					.findByUserIdAndName(userId, newCategoryName);

			Long categoryId;

			if (existingCategory.isPresent()) {

				categoryId = existingCategory.get().getCategoryId();

				// 既存カテゴリーにも、今回選択された親カテゴリーとの経路を追加する。
				categoryRelationService.addCategoryRelations(
						categoryId,
						parentCategoryIds,
						userId);

			} else {

				categoryId = articleCategoryService.insertCategory(
						userId,
						newCategoryName,
						parentCategoryIds,
						newCategoryDisplayName);
			}

			// categorySelectに新規カテゴリー名が含まれていれば、そのPathを使用する。
			String newCategoryPath = categorySelect == null
					|| categorySelect.isBlank()
							? newCategoryName
							: categorySelect.substring(
									categorySelect.lastIndexOf("/") + 1)
									.equals(newCategoryName)
											? categorySelect
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
