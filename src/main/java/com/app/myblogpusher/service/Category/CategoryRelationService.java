/**
 * カテゴリー同士の親子関係を管理するサービス
 *
 * ArticleCategoryはカテゴリーそのものを管理し、
 * CategoryRelationはカテゴリーがどの親カテゴリー配下に存在するか、
 * また、その関係がどのカテゴリー経路に属するかを管理する。
 */

package com.app.myblogpusher.service.Category;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.Article.ArticleCategoryRepository;
import com.app.myblogpusher.service.Article.ArticleWorkService;

@Service
public class CategoryRelationService {

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	@Autowired
	private ArticleWorkService articleWorkService;

	/**
	 * カテゴリーと親カテゴリーの直接の関係を登録する。
	 *
	 * groupIdが指定されている場合は既存のgroupIdを使用する。
	 * groupIdが未指定の場合は採番せず、nullのまま登録する。
	 */
	public void addRelation(
			Long categoryId,
			Long parentCategoryId,
			Long groupId,
			String categoryPath,
			Long userId) {

		if (categoryRelationRepository
				.existsByCategoryIdAndParentCategoryIdAndCategoryPath(
						categoryId,
						parentCategoryId,
						categoryPath)) {
			return;
		}

		// 同じカテゴリー・親カテゴリー・カテゴリー経路が
		// すでに存在する場合は重複登録しない。
		boolean exists = categoryRelationRepository
				.findByCategoryId(categoryId)
				.stream()
				.anyMatch(relation -> java.util.Objects.equals(
						parentCategoryId,
						relation.getParentCategoryId())
						&& categoryPath.equals(relation.getCategoryPath()));

		if (exists) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();

		CategoryRelation relation = new CategoryRelation();

		relation.setGroupId(groupId);
		relation.setCategoryId(categoryId);
		relation.setParentCategoryId(parentCategoryId);
		relation.setCategoryPath(categoryPath);

		relation.setCreateDate(now);
		relation.setUpdateDate(now);
		relation.setCreateUser(userId);
		relation.setUpdateUser(userId);

		categoryRelationRepository.save(relation);
	}

	/**
	 * カテゴリーと親カテゴリーの直接の親子関係を登録する。
	 *
	 * 祖先カテゴリーの経路は生成せず、
	 * 指定された親カテゴリーと子カテゴリーの関係のみを登録する。
	 */
	@Async
	public void addCategoryRelations(
			Long categoryId,
			List<Long> parentCategoryIds,
			Long userId) {

		ArticleCategory category = articleCategoryRepository
				.findById(categoryId)
				.orElseThrow();

		// 親カテゴリーがない場合は、最上位カテゴリーとして登録する。
		if (parentCategoryIds == null || parentCategoryIds.isEmpty()) {

			addRelation(
					categoryId,
					null,
					null,
					category.getCategoryName(),
					userId);

			return;
		}

		// 指定された親カテゴリーとの直接の関係だけを登録する。
		for (Long parentCategoryId : parentCategoryIds) {

			ArticleCategory parentCategory = articleCategoryRepository
					.findById(parentCategoryId)
					.orElseThrow();

			String categoryPath = parentCategory.getCategoryName()
					+ "/" + category.getCategoryName();

			addRelation(
					categoryId,
					parentCategoryId,
					null,
					categoryPath,
					userId);
		}
	}

	/**
	 * 指定されたカテゴリー経路のgroupIdをArticleWorkへ反映する。
	 *
	 * 記事のhugoPathに対応するCategoryRelationを特定し、
	 * 編集画面で使用するgroupIdを設定する。
	 */
	@Async
	public void updateArticleWorkCategoryGroupId(
	        Long workId,
	        Long categoryId,
	        Long parentCategoryId,
	        Long userId) {

		for (int i = 0; i < 20; i++) {

			CategoryRelation relation = categoryRelationRepository
			        .findByCategoryId(categoryId)
			        .stream()
			        .filter(item -> java.util.Objects.equals(
			                parentCategoryId,
			                item.getParentCategoryId()))
			        .findFirst()
			        .orElse(null);

			if (relation != null && relation.getGroupId() != null) {

				articleWorkService.updateCategoryGroupId(
						workId,
						relation.getGroupId(),
						userId);

				return;
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/**
	 * 指定カテゴリーの親子関係をすべて削除する。
	 */
	public void deleteRelationsByCategoryId(Long categoryId) {

		List<CategoryRelation> relations = categoryRelationRepository
				.findByCategoryId(categoryId);

		categoryRelationRepository.deleteAll(relations);
	}

	/**
	 * 指定カテゴリーを親としている関係をすべて削除する。
	 */
	public void deleteRelationsByParentCategoryId(Long parentCategoryId) {

		List<CategoryRelation> relations = categoryRelationRepository
				.findByParentCategoryId(parentCategoryId);

		categoryRelationRepository.deleteAll(relations);
	}

	/**
	 * カテゴリーの親子関係を更新する。
	 *
	 * 既存のCategoryRelationを全削除して作り直すのではなく、
	 * 変更後も使用する経路はgroupIdを維持したまま更新する。
	 *
	 * 不要になった経路だけ削除し、新しく必要になった経路だけ追加する。
	 */
	public void updateRelations(
			Long categoryId,
			List<Long> parentCategoryIds,
			Long userId) {

		ArticleCategory category = articleCategoryRepository
				.findById(categoryId)
				.orElseThrow();

		List<Long> newParentIds = parentCategoryIds == null
				? List.of()
				: parentCategoryIds;

		List<CategoryRelation> existingRelations = new ArrayList<>(
				categoryRelationRepository
						.findByCategoryId(categoryId));

		List<CategoryRelation> usedRelations = new ArrayList<>();

		/*
		 * 変更後に必要となる直接の親子関係だけを登録する。
		 */
		for (Long parentCategoryId : newParentIds) {

			ArticleCategory parentCategory = articleCategoryRepository
					.findById(parentCategoryId)
					.orElseThrow();

			String categoryPath = parentCategory.getCategoryName()
					+ "/"
					+ category.getCategoryName();

			updateOrCreateRelation(
					categoryId,
					parentCategoryId,
					categoryPath,
					existingRelations,
					usedRelations,
					userId);
		}
	}

	private void updateOrCreateRelation(
			Long categoryId,
			Long parentCategoryId,
			String categoryPath,
			List<CategoryRelation> existingRelations,
			List<CategoryRelation> usedRelations,
			Long userId) {

		CategoryRelation existing = existingRelations.stream()
		        .filter(relation -> java.util.Objects.equals(
		                parentCategoryId,
		                relation.getParentCategoryId()))
		        .findFirst()
		        .orElse(null);

		if (existing != null) {
			usedRelations.add(existing);
			return;
		}

		CategoryRelation relation = new CategoryRelation();
		relation.setCategoryId(categoryId);
		relation.setParentCategoryId(parentCategoryId);
		relation.setCategoryPath(categoryPath);
		relation.setCreateDate(LocalDateTime.now());
		relation.setUpdateDate(LocalDateTime.now());
		relation.setCreateUser(userId);
		relation.setUpdateUser(userId);

		categoryRelationRepository.save(relation);
		usedRelations.add(relation);
	}

	/**
	 * 指定されたカテゴリー経路の参考文献管理用groupIdを取得する。
	 *
	 * 参考文献はルートカテゴリー直下のカテゴリー単位で管理する。
	 * 例えば movie/batman/gadget の場合は、
	 * movie/batman のgroupIdを返す。
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
}
