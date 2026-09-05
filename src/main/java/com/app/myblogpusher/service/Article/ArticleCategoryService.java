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

		// 選択された親カテゴリーごとに親子関係を登録する。
		if (parentCategoryIds != null) {

			for (Long parentCategoryId : parentCategoryIds) {

				addCategoryRelation(
						newCategory.getCategoryId(),
						parentCategoryId);
			}
		}

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

		/*
		 * カテゴリーの親子関係はCategoryRelationを基準にする。
		 *
		 * 1つのカテゴリーが複数の親を持てるため、
		 * categoryIdごとに複数のparentCategoryIdを取得する。
		 */
		List<CategoryRelation> relations = categoryRelationRepository.findAll();

		return categories.stream()
				.map(c -> {

					List<Long> parentCategoryIds = relations.stream()
							.filter(relation -> relation.getCategoryId().equals(c.getCategoryId()))
							.map(CategoryRelation::getParentCategoryId)
							.toList();

					String parentCategoryName = parentCategoryIds.stream()
							.map(categoryNameMap::get)
							.filter(name -> name != null)
							.collect(Collectors.joining(", "));

					return new CategoryDictionaryView(
							c.getCategoryId(),
							c.getCategoryName(),
							parentCategoryIds,
							parentCategoryName.isEmpty()
									? null
									: parentCategoryName,
							c.getDisplayName(),
							countMap.getOrDefault(c.getCategoryId(), 0L));
				})
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

		// 既存の親子関係を削除して、選択された親との関係を登録する。
		categoryRelationService.deleteRelationsByCategoryId(categoryId);

		if (parentCategoryIds != null) {

			for (Long parentCategoryId : parentCategoryIds) {

				addCategoryRelation(
						categoryId,
						parentCategoryId);
			}
		}
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
	 * ルートからのフルパス付きでカテゴリー一覧を返す。
	 *
	 * 親カテゴリーを持つカテゴリーのうち、
	 * 子カテゴリーを持たない末端カテゴリーだけを選択肢にする。
	 */
	public List<CategoryOptionView> findSelectableCategories(Long userId) {

		List<ArticleCategory> categories = articleCategoryRepository.findByUserId(userId);

		if (categories.isEmpty()) {
			return List.of();
		}

		/*
		 * CategoryRelationからカテゴリー経路を取得する。
		 *
		 * ArticleCategory自身はカテゴリーそのものを表すため、
		 * 実際の階層構造とカテゴリー経路はcategory_relationを基準にする。
		 */
		List<CategoryRelation> relations = categoryRelationRepository.findAll();

		List<CategoryOptionView> result = new ArrayList<>();

		/*
		 * category_relationに登録されているカテゴリー経路を
		 * 記事投稿画面の選択肢として使用する。
		 *
		 * 親カテゴリーを持たないルートカテゴリーは
		 * category_relationに存在しないため対象外になる。
		 *
		 * また、自分のcategory_pathを親として持つ経路が存在する場合は
		 * 途中カテゴリーなので選択肢から除外する。
		 */
		relations.stream()
				.filter(relation -> relation.getCategoryPath() != null
						&& !relation.getCategoryPath().isBlank())
				.filter(relation -> categories.stream()
						.anyMatch(category -> category.getCategoryId()
								.equals(relation.getCategoryId())))
				.filter(relation -> relations.stream()
						.noneMatch(childRelation -> {

							String childPath = childRelation.getCategoryPath();
							String currentPath = relation.getCategoryPath();

							if (childPath == null || currentPath == null) {
								return false;
							}

							return childPath.startsWith(currentPath + "/");
						}))
				.sorted((a, b) -> a.getCategoryPath()
						.compareToIgnoreCase(b.getCategoryPath()))
				.forEach(relation -> result.add(
						new CategoryOptionView(
								relation.getGroupId(),
								relation.getCategoryId(),
								relation.getCategoryPath())));

		return result;
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
								.anyMatch(relation -> relation.getCategoryId().equals(c.getCategoryId())))
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
	 * category_relationのgroup_idで実際のカテゴリー経路を特定し、
	 * その経路の第2階層カテゴリーをリンク検索対象とする。
	 */
	public Long findLinkSearchCategoryId(
			Long userId,
			Long groupId) {

		if (userId == null || groupId == null) {
			return null;
		}

		CategoryRelation relation = categoryRelationRepository.findAll()
				.stream()
				.filter(r -> groupId.equals(r.getGroupId()))
				.findFirst()
				.orElse(null);

		if (relation == null
				|| relation.getCategoryPath() == null
				|| relation.getCategoryPath().isBlank()) {
			return null;
		}

		String[] pathParts = relation.getCategoryPath().split("/");

		if (pathParts.length < 2) {
			return null;
		}

		String secondCategoryName = pathParts[1];

		return articleCategoryRepository.findByUserId(userId)
				.stream()
				.filter(category -> getCategoryLabel(category)
						.equals(secondCategoryName))
				.map(ArticleCategory::getCategoryId)
				.findFirst()
				.orElse(null);
	}

	/**
	 * 記事リンク検索用カテゴリーのHugoパスを取得する。
	 *
	 * category_relationのgroup_idに登録されている
	 * category_pathを基準に、第2階層までの経路を取得する。
	 */
	public String findLinkSearchCategoryPath(Long groupId) {

		if (groupId == null) {
			return null;
		}

		CategoryRelation relation = categoryRelationRepository.findAll()
				.stream()
				.filter(r -> groupId.equals(r.getGroupId()))
				.findFirst()
				.orElse(null);

		if (relation == null
				|| relation.getCategoryPath() == null
				|| relation.getCategoryPath().isBlank()) {
			return null;
		}

		String[] pathParts = relation.getCategoryPath().split("/");

		if (pathParts.length == 0) {
			return null;
		}

		if (pathParts.length == 1) {
			return pathParts[0];
		}

		return pathParts[0] + "/" + pathParts[1];
	}

	/**
	 * category_group_idからカテゴリー経路を取得する。
	 *
	 * 下書き一覧など、記事が選択したカテゴリー経路を表示する場合に使用する。
	 */
	public String findCategoryPathByGroupId(Long groupId) {

		if (groupId == null) {
			return null;
		}

		return categoryRelationRepository.findByGroupId(groupId)
				.stream()
				.map(CategoryRelation::getCategoryPath)
				.filter(path -> path != null && !path.isBlank())
				.findFirst()
				.orElse(null);
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

	/**
	 * カテゴリーと親カテゴリーの関係を登録する。
	 *
	 * 親カテゴリーが持つすべてのcategory_pathを基準に、
	 * 子カテゴリーを追加した経路を生成する。
	 *
	 * 既に同じcategory_pathが存在する場合はgroup_idを再利用し、
	 * 存在しない経路の場合はgroup_idを指定せず、
	 * PostgreSQLのserialによる自動採番で新規登録する。
	 */
	private void addCategoryRelation(
	        Long categoryId,
	        Long parentCategoryId) {

	    ArticleCategory category = articleCategoryRepository
	            .findById(categoryId)
	            .orElseThrow();

	    List<CategoryRelation> parentRelations = categoryRelationRepository
	            .findByCategoryId(parentCategoryId);

	    /*
	     * 親カテゴリー自身がcategory_relationに存在しない場合は、
	     * ルートカテゴリーの子として直接パスを作る。
	     */
	    if (parentRelations.isEmpty()) {

	        ArticleCategory parentCategory = articleCategoryRepository
	                .findById(parentCategoryId)
	                .orElseThrow();

	        String categoryPath = parentCategory.getCategoryName()
	                + "/"
	                + category.getCategoryName();

	        CategoryRelation existingRelation = categoryRelationRepository
	                .findByCategoryPath(categoryPath)
	                .stream()
	                .findFirst()
	                .orElse(null);

	        categoryRelationService.addRelation(
	                categoryId,
	                parentCategoryId,
	                existingRelation == null
	                        ? null
	                        : existingRelation.getGroupId(),
	                categoryPath);

	        return;
	    }

	    /*
	     * 親カテゴリーが持つすべての経路を引き継ぎ、
	     * その末尾に現在のカテゴリーを追加する。
	     *
	     * 例:
	     * character/batman
	     * game/injustice/character
	     *
	     * → character/batman/command
	     * → game/injustice/character/command
	     */
	    for (CategoryRelation parentRelation : parentRelations) {

	        String parentPath = parentRelation.getCategoryPath();

	        if (parentPath == null || parentPath.isBlank()) {
	            continue;
	        }

	        String categoryPath = parentPath
	                + "/"
	                + category.getCategoryName();

	        /*
	         * 同じcategory_pathが既に存在する場合は、
	         * 既存のgroup_idをそのまま使用する。
	         *
	         * 存在しない場合はgroupIdをnullで渡し、
	         * PostgreSQLのserialで新しいgroup_idを採番する。
	         */
	        CategoryRelation existingRelation = categoryRelationRepository
	                .findByCategoryPath(categoryPath)
	                .stream()
	                .findFirst()
	                .orElse(null);

	        categoryRelationService.addRelation(
	                categoryId,
	                parentCategoryId,
	                existingRelation == null
	                        ? null
	                        : existingRelation.getGroupId(),
	                categoryPath);
	    }
	}
}