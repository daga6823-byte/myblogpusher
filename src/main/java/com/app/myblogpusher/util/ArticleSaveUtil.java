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

	// -----------------------------------------------------
	// カテゴリー情報の解決
	//
	// 選択されたカテゴリーと新規カテゴリー入力から、
	// カテゴリー登録・親カテゴリー情報を解決する。
	// -----------------------------------------------------
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
			        hugoPath,
			        title,
			        formattedContent);

			if (existing.isPresent()) {
				return existing.get().getWorkId();
			}

			Long newWorkId = articleWorkService.insertArticleWork(
					userId,
					null,
					hugoPath,
					null,
					title,
					formattedContent,
					slug);

			return newWorkId;

		} else {

			typoCategoryId = articleWorkService.findById(workId, userId)
					.getTypoCategoryId();

			articleWorkService.updateArticleWork(
					workId,
					hugoPath,
					typoCategoryId,
					title,
					formattedContent,
					userId,
					slug);

			return workId;
		}
	}

	/**
	 * カテゴリー選択値を解決する。
	 *
	 * 新規カテゴリーの場合はカテゴリーの存在確認と登録を行う。
	 * 記事保存ではgroupIdを使用せず、カテゴリー階層はhugoPathで管理する。
	 */
	private CategoryResolution resolveCategory(
			Long userId,
			String categorySelect,
			String newCategoryName,
			String newCategoryDisplayName) {

		if (newCategoryName == null || newCategoryName.isBlank()) {
			return new CategoryResolution(null);
		}

		// categorySelectに新規カテゴリー名が含まれている場合は、
		// 末尾を除いたPathから親カテゴリーIDを取得する。
		String parentCategoryPath = categorySelect;

		if (parentCategoryPath != null && !parentCategoryPath.isBlank()) {

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

		return new CategoryResolution(categoryId);
	}

	/**
	 * カテゴリー解決結果を保持する。
	 */
	private record CategoryResolution(Long categoryId) {
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
