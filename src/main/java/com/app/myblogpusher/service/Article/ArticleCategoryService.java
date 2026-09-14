package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.TypoCorrectionRepository;
import com.app.myblogpusher.repository.Article.ArticleCategoryRepository;
import com.app.myblogpusher.service.Category.CategoryRelationService;

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

	@Autowired
	private CategoryRelationService categoryRelationService;

	/**
	 * カテゴリーを新規登録する
	 *
	 * カテゴリー名の入力チェック、
	 * 重複チェックを行った上で登録する。
	 */
	public Long insertCategory(
			Long userId,
			String categoryName,
			List<Long> parentCategoryIds,
			String displayName) {

		if (categoryName == null || categoryName.isBlank()) {
			throw new IllegalArgumentException("カテゴリー名を入力してください");
		}

		if (findByUserIdAndName(userId, categoryName).isPresent()) {
			throw new IllegalArgumentException("同じ名前のカテゴリーが既に存在します");
		}

		ArticleCategory newCategory = new ArticleCategory();
		newCategory.setUserId(userId);
		newCategory.setCategoryName(categoryName);
		newCategory.setCreateDate(LocalDateTime.now());
		newCategory.setUpdateDate(LocalDateTime.now());
		newCategory.setCreateUser(userId);
		newCategory.setUpdateUser(userId);

		// 移行期間中は旧カラムにも保持する。
		newCategory.setParentCategoryId(
				parentCategoryIds == null || parentCategoryIds.isEmpty()
						? null
						: parentCategoryIds.get(0));

		newCategory.setDisplayName(displayName);

		articleCategoryRepository.save(newCategory);

		if (parentCategoryIds != null) {

			categoryRelationService.addCategoryRelations(
					newCategory.getCategoryId(),
					parentCategoryIds,
					userId);
		}

		return newCategory.getCategoryId();
	}

	public Optional<ArticleCategory> findById(Long categoryId) {
		return articleCategoryRepository.findById(categoryId);
	}

	/**
	 * カテゴリー情報を更新する
	 *
	 * カテゴリー名・親カテゴリー・表示名を更新する。
	 */
	public void update(
			Long categoryId,
			Long userId,
			String categoryName,
			List<Long> parentCategoryIds,
			String displayName) {

		if (categoryName == null || categoryName.isBlank()) {
			throw new IllegalArgumentException("カテゴリー名を入力してください");
		}

		Optional<ArticleCategory> existing = findByUserIdAndName(userId, categoryName);

		if (existing.isPresent()
				&& !existing.get().getCategoryId().equals(categoryId)) {
			throw new IllegalArgumentException("同じ名前のカテゴリーが既に存在します");
		}

		ArticleCategory category = articleCategoryRepository.findById(categoryId)
				.orElseThrow();

		if (!category.getUserId().equals(userId)) {
			throw new IllegalStateException("他のユーザーのカテゴリーは変更できません");
		}

		category.setCategoryName(categoryName);

		// 移行期間中は旧カラムにも保持する。
		category.setParentCategoryId(
				parentCategoryIds == null || parentCategoryIds.isEmpty()
						? null
						: parentCategoryIds.get(0));

		category.setDisplayName(displayName);
		category.setUpdateUser(userId);
		category.setUpdateDate(LocalDateTime.now());

		articleCategoryRepository.save(category);

		// 選択された親とのカテゴリー経路を追加登録する。
		// 既存のRelationは削除・変更しない。
		categoryRelationService.addCategoryRelations(
				categoryId,
				parentCategoryIds,
				userId);
	}

	@Autowired
	private TypoCorrectionRepository typoCorrectionRepository;

	public void delete(Long categoryId, Long userId) {

		ArticleCategory category = articleCategoryRepository.findById(categoryId)
				.orElseThrow();

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

		// このカテゴリー自身が持つ親との関係を削除する。
		categoryRelationService.deleteRelationsByCategoryId(categoryId);

		// このカテゴリーを親としている子カテゴリー側の関係も削除する。
		categoryRelationService.deleteRelationsByParentCategoryId(categoryId);

		articleCategoryRepository.delete(category);
	}
}