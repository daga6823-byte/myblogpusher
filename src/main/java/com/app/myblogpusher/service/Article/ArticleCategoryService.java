package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Category.CategoryDictionaryView;
import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.TypoCorrectionRepository;
import com.app.myblogpusher.repository.Article.ArticleCategoryRepository;
import com.app.myblogpusher.service.CategoryRelationService;

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

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	/**
	 * カテゴリーを新規登録する
	 *
	 * カテゴリー名の入力チェック、
	 * 重複チェックを行った上で登録する。
	 */
	public Long insertCategory(
			Long userId,
			String categoryName,
			Long parentCategoryId,
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
		newCategory.setParentCategoryId(parentCategoryId);

		newCategory.setDisplayName(displayName);

		articleCategoryRepository.save(newCategory);

		// 新しいカテゴリー関係テーブルにも親子関係を登録する。
		categoryRelationService.addRelation(
				newCategory.getCategoryId(),
				parentCategoryId);

		return newCategory.getCategoryId();
	}

	public Optional<ArticleCategory> findById(Long categoryId) {
		return articleCategoryRepository.findById(categoryId);
	}

	/**
	 * カテゴリー辞典表示用の一覧を取得する
	 *
	 * 誤字登録件数と親カテゴリー表示名を付加して返す。
	 */
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
						c.getParentCategoryId() == null
								? null
								: categoryNameMap.get(c.getParentCategoryId()),
						c.getDisplayName(),
						countMap.getOrDefault(c.getCategoryId(), 0L)))
				.toList();
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
			Long parentCategoryId,
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
		category.setParentCategoryId(parentCategoryId);

		category.setDisplayName(displayName);
		category.setUpdateUser(userId);
		category.setUpdateDate(LocalDateTime.now());

		articleCategoryRepository.save(category);

		// 旧い関係を削除して、新しい親との関係を登録する。
		categoryRelationService.deleteRelationsByCategoryId(categoryId);
		categoryRelationService.addRelation(categoryId, parentCategoryId);
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

	/**
	 * 記事投稿画面のカテゴリー選択プルダウン用に、
	 * ルートからのフルパス付きでカテゴリー一覧を返す（sortOrder順の深さ優先）
	 */
	public List<CategoryOptionView> findSelectableCategories(Long userId) {

		List<ArticleCategory> categories = articleCategoryRepository.findByUserId(userId);

		if (categories.isEmpty()) {
			return List.of();
		}

		/*
		 * CategoryRelationから親子関係を構築する。
		 *
		 * ArticleCategory自身はカテゴリーそのものを表すため、
		 * 階層構造はcategory_relationを基準にする。
		 */
		List<CategoryRelation> relations = categoryRelationRepository.findAll();

		Map<Long, ArticleCategory> categoryMap = categories.stream()
				.collect(Collectors.toMap(
						ArticleCategory::getCategoryId,
						c -> c));

		Map<Long, List<ArticleCategory>> childrenByParent = new HashMap<>();

		for (CategoryRelation relation : relations) {

			ArticleCategory child = categoryMap.get(relation.getCategoryId());
			ArticleCategory parent = categoryMap.get(relation.getParentCategoryId());

			// 他ユーザーのカテゴリー関係は対象外にする。
			if (child == null || parent == null) {
				continue;
			}

			childrenByParent
					.computeIfAbsent(parent.getCategoryId(), k -> new ArrayList<>())
					.add(child);
		}

		/*
		 * category_relationに親が登録されていないカテゴリーを
		 * ルートカテゴリーとして扱う。
		 */
		List<ArticleCategory> roots = categories.stream()
				.filter(category -> !hasParentRelation(
						category.getCategoryId(),
						relations))
				.collect(Collectors.toCollection(ArrayList::new));

		roots.sort((a, b) -> compareSortOrder(
				a.getSortOrder(),
				b.getSortOrder()));

		childrenByParent.values()
				.forEach(list -> list.sort((a, b) -> compareSortOrder(
						a.getSortOrder(),
						b.getSortOrder())));

		List<CategoryOptionView> result = new ArrayList<>();

		for (ArticleCategory root : roots) {
			appendOption(root, "", childrenByParent, result);
		}

		return result;
	}

	/**
	 * 指定カテゴリーに親カテゴリーとの関係が存在するか確認する。
	 */
	private boolean hasParentRelation(
			Long categoryId,
			List<CategoryRelation> relations) {

		return relations.stream()
				.anyMatch(relation -> relation.getCategoryId().equals(categoryId));
	}

	private void appendOption(
			ArticleCategory current,
			String parentPath,
			Map<Long, List<ArticleCategory>> childrenByParent,
			List<CategoryOptionView> result) {

		String label = (current.getDisplayName() != null
				&& !current.getDisplayName().isBlank())
						? current.getDisplayName()
						: current.getCategoryName();

		String fullPath = parentPath.isEmpty()
				? label
				: parentPath + "/" + label;

		result.add(new CategoryOptionView(
				current.getCategoryId(),
				fullPath));

		List<ArticleCategory> children = childrenByParent.getOrDefault(
				current.getCategoryId(),
				List.of());

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

	/**
	 * フルパスからカテゴリーIDを取得する。
	 *
	 * CategoryRelationを使用して親子関係を辿る。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * → gadgetのcategoryId
	 */
	public Long findCategoryIdByFullPath(
			Long userId,
			String fullPath) {

		if (userId == null || fullPath == null || fullPath.isBlank()) {
			return null;
		}

		List<ArticleCategory> categories = articleCategoryRepository.findByUserId(userId);

		if (categories.isEmpty()) {
			return null;
		}

		Map<Long, ArticleCategory> categoryMap = categories.stream()
				.collect(Collectors.toMap(
						ArticleCategory::getCategoryId,
						c -> c));

		Map<Long, List<Long>> childrenMap = new HashMap<>();

		List<CategoryRelation> relations = categoryRelationRepository.findAll();

		for (CategoryRelation relation : relations) {

			Long categoryId = relation.getCategoryId();
			Long parentCategoryId = relation.getParentCategoryId();

			if (!categoryMap.containsKey(categoryId)
					|| !categoryMap.containsKey(parentCategoryId)) {
				continue;
			}

			childrenMap
					.computeIfAbsent(parentCategoryId, k -> new ArrayList<>())
					.add(categoryId);
		}

		String[] pathParts = fullPath.split("/");

		ArticleCategory current = null;

		for (int i = 0; i < pathParts.length; i++) {

			String name = pathParts[i];

			if (i == 0) {

				current = categories.stream()
						.filter(c -> !relations.stream()
								.anyMatch(relation ->
										relation.getCategoryId().equals(c.getCategoryId())))
						.filter(c -> getCategoryLabel(c).equals(name))
						.findFirst()
						.orElse(null);

			} else {

				if (current == null) {
					return null;
				}

				List<Long> childIds = childrenMap.getOrDefault(
						current.getCategoryId(),
						List.of());

				current = childIds.stream()
						.map(categoryMap::get)
						.filter(c -> c != null)
						.filter(c -> getCategoryLabel(c).equals(name))
						.findFirst()
						.orElse(null);
			}

			if (current == null) {
				return null;
			}
		}

		return current.getCategoryId();
	}

	/**
	 * 記事リンク検索用カテゴリーIDを取得する。
	 *
	 * 第2階層のカテゴリーをリンク検索対象とする。
	 */
	public Long findLinkSearchCategoryId(Long categoryId) {

		return findReferenceCategoryId(categoryId);
	}

	/**
	 * 記事リンク検索用カテゴリーのHugoパスを取得する。
	 *
	 * カテゴリーIDから第2階層までのカテゴリー経路を組み立てる。
	 */
	public String findLinkSearchCategoryPath(Long categoryId) {

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
			return getCategoryLabel(category);
		}

		ArticleCategory parent = articleCategoryRepository
				.findById(category.getParentCategoryId())
				.orElse(null);

		if (parent == null) {
			return getCategoryLabel(category);
		}

		if (parent.getParentCategoryId() == null) {
			return getCategoryLabel(parent) + "/" + getCategoryLabel(category);
		}

		ArticleCategory root = articleCategoryRepository
				.findById(parent.getParentCategoryId())
				.orElse(null);

		if (root == null) {
			return getCategoryLabel(parent) + "/" + getCategoryLabel(category);
		}

		return getCategoryLabel(root)
				+ "/"
				+ getCategoryLabel(parent);
	}

	/**
	 * カテゴリーの表示用ラベルを取得する。
	 *
	 * displayNameが設定されている場合はdisplayName、
	 * 未設定の場合はcategoryNameを使用する。
	 */
	private String getCategoryLabel(ArticleCategory category) {

		if (category.getDisplayName() != null
				&& !category.getDisplayName().isBlank()) {
			return category.getDisplayName();
		}

		return category.getCategoryName();
	}

}