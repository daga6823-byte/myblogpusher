/**
 * カテゴリー経路の解決を担当するService
 *
 * CategoryRelationを基準として、
 * groupIdとカテゴリーID、カテゴリー経路の相互変換を行う。
 *
 * ArticleCategoryそのもののCRUDは担当しない。
 */

package com.app.myblogpusher.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.repository.CategoryRelationRepository;

@Service
public class CategoryPathService {

	private final CategoryRelationRepository categoryRelationRepository;

	public CategoryPathService(
			CategoryRelationRepository categoryRelationRepository) {

		this.categoryRelationRepository = categoryRelationRepository;
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
	 * カテゴリー経路のgroupIdから、
	 * 添削・誤字検索で使用する第2階層のcategoryIdを取得する。
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * → movie/batman
	 * → batmanのcategoryId
	 */
	public Long findTypoCategoryIdByGroupId(Long groupId) {

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
			return null;
		}

		String referencePath = pathParts[0] + "/" + pathParts[1];

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

		if (groupId == null) {
			return null;
		}

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
	 * カテゴリー経路からCategoryRelationを取得する。
	 */
	public List<CategoryRelation> findByGroupId(Long groupId) {

		if (groupId == null) {
			return List.of();
		}

		return categoryRelationRepository.findByGroupId(groupId);
	}
}