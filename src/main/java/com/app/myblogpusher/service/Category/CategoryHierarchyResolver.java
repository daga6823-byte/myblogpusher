// カテゴリーの「第2階層」を求める業務ルールを担当するサービス。
// 辞書検索対象・参考文献登録対象カテゴリーの解決に使用する。
package com.app.myblogpusher.service.Category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.Article.ArticleCategoryRepository;

@Service
public class CategoryHierarchyResolver {

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	/**
	 * 辞書検索に使用するカテゴリーIDを取得する。
	 *
	 * 第2階層のカテゴリーを辞書検索対象とする。
	 */
	public Long findDictionaryCategoryId(Long categoryId) {

		if (categoryId == null) {
			return null;
		}

		ArticleCategory category = articleCategoryRepository
				.findById(categoryId)
				.orElse(null);

		if (category == null || category.getParentCategoryId() == null) {
			return null;
		}

		ArticleCategory parent = articleCategoryRepository
				.findById(category.getParentCategoryId())
				.orElse(null);

		if (parent == null) {
			return null;
		}

		if (parent.getParentCategoryId() == null) {
			return category.getCategoryId();
		}

		return parent.getCategoryId();
	}

	/**
	 * 記事カテゴリーから参考文献登録対象カテゴリーを取得する。
	 *
	 * 第2階層のカテゴリーを参考文献登録対象とする。
	 */
	public Long findReferenceCategoryId(Long categoryId) {

		if (categoryId == null) {
			return null;
		}

		ArticleCategory category = articleCategoryRepository
				.findById(categoryId)
				.orElse(null);

		if (category == null) {
			return null;
		}

		if (category.getParentCategoryId() == null) {
			return category.getCategoryId();
		}

		ArticleCategory parent = articleCategoryRepository
				.findById(category.getParentCategoryId())
				.orElse(null);

		if (parent == null) {
			return null;
		}

		if (parent.getParentCategoryId() == null) {
			return category.getCategoryId();
		}

		return parent.getCategoryId();
	}
}