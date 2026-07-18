package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.CategoryDictionaryView;
import com.app.myblogpusher.dto.CategoryOptionView;
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

		System.out.println("insertCategory");
		System.out.println("categoryName = " + categoryName);
		System.out.println("displayName = " + displayName);
		System.out.println("parentCategoryId = " + parentCategoryId);

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
						c -> c.getDisplayName() == null
								? c.getCategoryName()
								: c.getDisplayName()));

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
		category.setUpdateDate(LocalDateTime.now());

		System.out.println("entity displayName = " + category.getDisplayName());
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

	/**
	 * 記事投稿画面のカテゴリー選択プルダウン用に、
	 * ルートからのフルパス付きでカテゴリー一覧を返す（sortOrder順の深さ優先）
	 */
	public List<CategoryOptionView> findSelectableCategories(Long userId) {
		List<ArticleCategory> categories = articleCategoryRepository.findByUserId(userId);
		if (categories.isEmpty()) {
			return List.of();
		}

		Map<Long, List<ArticleCategory>> childrenByParent = new HashMap<>();
		List<ArticleCategory> roots = new ArrayList<>();

		for (ArticleCategory c : categories) {
			if (c.getParentCategoryId() == null) {
				roots.add(c);
			} else {
				childrenByParent
						.computeIfAbsent(c.getParentCategoryId(), k -> new ArrayList<>())
						.add(c);
			}
		}

		roots.sort((a, b) -> compareSortOrder(a.getSortOrder(), b.getSortOrder()));
		childrenByParent.values()
				.forEach(list -> list.sort((a, b) -> compareSortOrder(a.getSortOrder(), b.getSortOrder())));

		List<CategoryOptionView> result = new ArrayList<>();
		for (ArticleCategory root : roots) {
			appendOption(root, "", childrenByParent, result);
		}
		return result;
	}

	private void appendOption(
			ArticleCategory current,
			String parentPath,
			Map<Long, List<ArticleCategory>> childrenByParent,
			List<CategoryOptionView> result) {

		String label = (current.getDisplayName() != null && !current.getDisplayName().isBlank())
				? current.getDisplayName()
				: current.getCategoryName();
		String fullPath = parentPath.isEmpty() ? label : parentPath + "/" + label;

		result.add(new CategoryOptionView(current.getCategoryId(), fullPath));

		List<ArticleCategory> children = childrenByParent.getOrDefault(current.getCategoryId(), List.of());
		for (ArticleCategory child : children) {
			appendOption(child, fullPath, childrenByParent, result);
		}
	}

	/**
	 * sortOrderの比較用。null(未設定)は0として扱う
	 */
	private int compareSortOrder(Integer a, Integer b) {
		int av = a == null ? 0 : a;
		int bv = b == null ? 0 : b;
		return Integer.compare(av, bv);
	}

	/**
	 * 辞書検索に使用するカテゴリーIDを取得する。
	 *
	 * ルートカテゴリーは対象外とし、
	 * 第2階層は自分自身、
	 * 第3階層以降は親カテゴリーを返す。
	 */
	public Long findDictionaryCategoryId(Long categoryId) {

		if (categoryId == null) {
			return null;
		}

		ArticleCategory category = articleCategoryRepository
				.findById(categoryId)
				.orElse(null);

		if (category == null || category.getParentCategoryId() == null) {
			// ルートカテゴリー
			return null;
		}

		ArticleCategory parent = articleCategoryRepository
				.findById(category.getParentCategoryId())
				.orElse(null);

		if (parent == null) {
			return null;
		}

		// 親がルートなら自分自身（第2階層）
		if (parent.getParentCategoryId() == null) {
			return category.getCategoryId();
		}

		// 第3階層以降は親カテゴリー
		return parent.getCategoryId();
	}

	/**
	 * 記事カテゴリーから参考文献登録対象カテゴリーを取得する
	 *
	 * ルート直下カテゴリーを返す。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * の場合
	 *
	 * batman
	 */
	public Long findReferenceCategoryId(Long categoryId) {

		ArticleCategory category = findById(categoryId)
				.orElseThrow();

		ArticleCategory current = category;

		while (current.getParentCategoryId() != null) {

			ArticleCategory parent = findById(current.getParentCategoryId())
					.orElseThrow();

			// 親がルートなら現在カテゴリーが対象
			if (parent.getParentCategoryId() == null) {

				return current.getCategoryId();
			}

			current = parent;
		}

		// ルートカテゴリーしかない場合
		return categoryId;
	}

}