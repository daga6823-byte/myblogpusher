/**

* カテゴリー経路の解決を担当するService
*
* CategoryRelationを基準として、
* groupIdとカテゴリーID、カテゴリー経路の相互変換を行う。
*
* ArticleCategoryそのもののCRUDは担当しない。
  */

package com.app.myblogpusher.service.Category;

import java.util.List;

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

		/*
		 * 2階層以上のカテゴリーはCategoryRelationのcategoryPathを
		 * 直接検索する。
		 *
		 * 同じカテゴリーが複数の親を持つ場合でも、
		 * 選択されたカテゴリー経路そのものから正しいcategoryIdを取得できる。
		 */
		if (fullPath.contains("/")) {

			return categoryRelationRepository
					.findByCategoryPath(fullPath)
					.stream()
					.filter(relation -> articleCategoryRepository
							.findById(relation.getCategoryId())
							.map(category -> userId.equals(category.getUserId()))
							.orElse(false))
					.map(CategoryRelation::getCategoryId)
					.findFirst()
					.orElse(null);
		}

		/*
		 * ルートカテゴリーにはCategoryRelationが存在しないため、
		 * categoryPathから直接取得することはできない。
		 *
		 * CategoryRelationの親として一度も登場していないカテゴリーを
		 * ルートカテゴリーとして判定する。
		 */
		List<ArticleCategory> categories = articleCategoryRepository.findByUserId(userId);

		List<CategoryRelation> relations = categoryRelationRepository.findAll();

		return categories.stream()
				.filter(category -> getCategoryLabel(category).equals(fullPath))
				.filter(category -> relations.stream()
						.noneMatch(relation -> relation.getCategoryId()
								.equals(category.getCategoryId())))
				.map(ArticleCategory::getCategoryId)
				.findFirst()
				.orElse(null);
	}

	/**
	 * フルカテゴリー経路からcategory_relationのgroupIdを取得する。
	 *
	 * GitHub同期など、カテゴリー経路を基準に
	 * Article.categoryGroupIdを解決する場合に使用する。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * → CategoryRelation.groupId
	 */
	public Long findGroupIdByFullPath(
			Long userId,
			String fullPath) {

		if (userId == null
				|| fullPath == null
				|| fullPath.isBlank()) {
			return null;
		}

		return categoryRelationRepository
				.findByCategoryPath(fullPath)
				.stream()
				.filter(relation -> articleCategoryRepository
						.findById(relation.getCategoryId())
						.map(category -> userId.equals(category.getUserId()))
						.orElse(false))
				.map(CategoryRelation::getGroupId)
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
