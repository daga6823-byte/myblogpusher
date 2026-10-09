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
	 * カテゴリーと親カテゴリーの関係を登録する。
	 *
	 * groupIdが指定されている場合は既存のgroupIdを使用し、
	 * 未指定の場合はPostgreSQLのserialによる自動採番に任せる。
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
				.anyMatch(relation -> parentCategoryId.equals(relation.getParentCategoryId())
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
	 * カテゴリーと親カテゴリーの関係を登録する。
	 *
	 * 親カテゴリーが持つすべてのcategory_pathを基準に、
	 * 子カテゴリーを追加した経路を生成する。
	 */
	@Async
	public void addCategoryRelations(
			Long categoryId,
			List<Long> parentCategoryIds,
			Long userId) {

		if (parentCategoryIds == null || parentCategoryIds.isEmpty())
			return;

		ArticleCategory category = articleCategoryRepository.findById(categoryId).orElseThrow();

		for (Long parentCategoryId : parentCategoryIds) {
			ArticleCategory parentCategory = articleCategoryRepository.findById(parentCategoryId).orElseThrow();
			List<CategoryRelation> parentRelations = categoryRelationRepository.findByCategoryId(parentCategoryId);

			if (parentRelations.isEmpty()) {
				String categoryPath = parentCategory.getCategoryName()
						+ "/" + category.getCategoryName();

				addRelation(categoryId, parentCategoryId, null, categoryPath, userId);
				continue;
			}

			for (CategoryRelation parentRelation : parentRelations) {
				String parentPath = parentRelation.getCategoryPath();
				if (parentPath == null || parentPath.isBlank())
					continue;

				String categoryPath = parentPath
						+ "/" + category.getCategoryName();

				addRelation(categoryId, parentCategoryId, null, categoryPath, userId);
			}
		}
	}

	/**
	 * 指定カテゴリーの親子関係を同期的に補完する。
	 *
	 * 通常の非同期登録でcategoryPathが作成されていない場合に使用する。
	 * 既存の関係は維持し、不足している経路だけを追加する。
	 */
	public void repairCategoryRelations(
			Long categoryId,
			List<Long> parentCategoryIds,
			Long userId) {

		if (parentCategoryIds == null || parentCategoryIds.isEmpty()) {
			return;
		}

		ArticleCategory category = articleCategoryRepository
				.findById(categoryId)
				.orElseThrow();

		for (Long parentCategoryId : parentCategoryIds) {

			ArticleCategory parentCategory = articleCategoryRepository
					.findById(parentCategoryId)
					.orElseThrow();

			List<CategoryRelation> parentRelations = categoryRelationRepository
					.findByCategoryId(parentCategoryId);

			if (parentRelations.isEmpty()) {

				String categoryPath = parentCategory.getCategoryName()
						+ "/" + category.getCategoryName();

				addRelation(
						categoryId,
						parentCategoryId,
						null,
						categoryPath,
						userId);

				continue;
			}

			for (CategoryRelation parentRelation : parentRelations) {

				String parentPath = parentRelation.getCategoryPath();

				if (parentPath == null || parentPath.isBlank()) {
					continue;
				}

				String categoryPath = parentPath
						+ "/" + category.getCategoryName();

				addRelation(
						categoryId,
						parentCategoryId,
						null,
						categoryPath,
						userId);
			}
		}
	}

	/**
	 * 非同期で登録されたカテゴリー経路のgroupIdをArticleWorkへ反映する。
	 *
	 * CategoryRelationはINSERT時にgroupIdがDBで採番されるため、
	 * ArticleWork保存時点でgroupIdが未確定の場合に後から設定する。
	 */
	@Async
	public void updateArticleWorkCategoryGroupId(
			Long workId,
			Long categoryId) {

		for (int i = 0; i < 20; i++) {

			List<CategoryRelation> relations = categoryRelationRepository.findByCategoryId(categoryId);

			Long groupId = relations.stream()
					.map(CategoryRelation::getGroupId)
					.filter(id -> id != null)
					.findFirst()
					.orElse(null);

			if (groupId != null) {
				articleWorkService.updateCategoryGroupId(
						workId,
						groupId);
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
		 * 変更後に必要となるカテゴリー経路を作成する。
		 */
		for (Long parentCategoryId : newParentIds) {

			ArticleCategory parentCategory = articleCategoryRepository
					.findById(parentCategoryId)
					.orElseThrow();

			List<CategoryRelation> parentRelations = categoryRelationRepository
					.findByCategoryId(parentCategoryId);

			if (parentRelations.isEmpty()) {

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

				continue;
			}

			for (CategoryRelation parentRelation : parentRelations) {

				String parentPath = parentRelation.getCategoryPath();

				if (parentPath == null || parentPath.isBlank()) {
					continue;
				}

				String categoryPath = parentPath
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
	}

	private void updateOrCreateRelation(
			Long categoryId,
			Long parentCategoryId,
			String categoryPath,
			List<CategoryRelation> existingRelations,
			List<CategoryRelation> usedRelations,
			Long userId) {

		CategoryRelation existing = existingRelations.stream()
				.filter(relation -> categoryPath.equals(
						relation.getCategoryPath()))
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
