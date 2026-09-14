/**

* カテゴリー経路の解決を担当するService
*
* CategoryRelationを基準として、
* groupIdとカテゴリーID、カテゴリー経路の相互変換を行う。
*
* ArticleCategoryそのもののCRUDは担当しない。
  */

package com.app.myblogpusher.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.Article.ArticleCategoryRepository;

@Service
public class CategoryPathService {

	private final CategoryRelationRepository categoryRelationRepository;

	private final ArticleCategoryRepository articleCategoryRepository;

	public CategoryPathService(
			CategoryRelationRepository categoryRelationRepository,
			ArticleCategoryRepository articleCategoryRepository) {

		this.categoryRelationRepository = categoryRelationRepository;
		this.articleCategoryRepository = articleCategoryRepository;
	}

	/**
	 * 指定されたカテゴリー経路の参考文献管理用groupIdを取得する。
	 *
	 * 参考文献はルートカテゴリー直下のカテゴリー単位で管理する。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * → movie/batman
	 */
	public Long resolveReferenceGroupId(Long groupId) {

		CategoryRelation relation = categoryRelationRepository
				.findByGroupId(groupId)
				.stream()
				.findFirst()
				.orElseThrow();

		String categoryPath = relation.getCategoryPath();

		String[] parts = categoryPath.split("/");

		if (parts.length <= 2) {
			return groupId;
		}

		String referencePath = parts[0] + "/" + parts[1];

		return categoryRelationRepository
				.findByCategoryPath(referencePath)
				.stream()
				.findFirst()
				.orElseThrow()
				.getGroupId();
	}

	/**
	 * 記事のgroupIdから参考文献管理用のcategoryIdを取得する。
	 *
	 * 現在の記事が深いカテゴリー経路に属している場合でも、
	 * ルート直下のカテゴリーを参考文献の共通カテゴリーとして使用する。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * → movie/batman
	 *
	 * → batmanのcategoryId
	 */
	public Long findReferenceCategoryIdByGroupId(Long groupId) {

		if (groupId == null) {
			return null;
		}

		CategoryRelation relation = categoryRelationRepository
				.findByGroupId(groupId)
				.stream()
				.findFirst()
				.orElse(null);

		if (relation == null
				|| relation.getCategoryPath() == null
				|| relation.getCategoryPath().isBlank()) {
			return null;
		}

		String[] pathParts = relation.getCategoryPath().split("/");

		if (pathParts.length < 2) {
			return relation.getCategoryId();
		}

		String referencePath = findSecondLevelPath(groupId);

		if (referencePath == null) {
			return null;
		}

		return categoryRelationRepository
				.findByCategoryPath(referencePath)
				.stream()
				.findFirst()
				.map(CategoryRelation::getCategoryId)
				.orElse(null);
	}

	/**
	 * カテゴリー経路のgroupIdから、
	 * 添削・誤字検索で使用する第2階層のcategoryIdを取得する。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * → movie/batman
	 *
	 * → batmanのcategoryId
	 */
	public Long findTypoCategoryIdByGroupId(Long groupId) {

		if (groupId == null) {
			return null;
		}

		String referencePath = findSecondLevelPath(groupId);

		if (referencePath == null) {
			return null;
		}

		return categoryRelationRepository
				.findByCategoryPath(referencePath)
				.stream()
				.findFirst()
				.map(CategoryRelation::getCategoryId)
				.orElse(null);
	}

	/**
	 * category_relationのgroupIdからカテゴリー経路を取得する。
	 */
	public String findCategoryPathByGroupId(Long groupId) {

		if (groupId == null) {
			return null;
		}

		return categoryRelationRepository
				.findByGroupId(groupId)
				.stream()
				.map(CategoryRelation::getCategoryPath)
				.filter(path -> path != null && !path.isBlank())
				.findFirst()
				.orElse(null);
	}

	/**
	 * 指定カテゴリー経路から第2階層までの経路を取得する。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * → movie/batman
	 */
	public String findSecondLevelPath(Long groupId) {

		String categoryPath = findCategoryPathByGroupId(groupId);

		if (categoryPath == null) {
			return null;
		}

		String[] pathParts = categoryPath.split("/");

		if (pathParts.length == 0) {
			return null;
		}

		if (pathParts.length == 1) {
			return pathParts[0];
		}

		return pathParts[0] + "/" + pathParts[1];
	}

	/**
	 * カテゴリー経路のgroupIdからCategoryRelationを取得する。
	 */
	public List<CategoryRelation> findByGroupId(Long groupId) {

		if (groupId == null) {
			return List.of();
		}

		return categoryRelationRepository.findByGroupId(groupId);
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
								.anyMatch(relation -> relation.getCategoryId()
										.equals(c.getCategoryId())))
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

}
