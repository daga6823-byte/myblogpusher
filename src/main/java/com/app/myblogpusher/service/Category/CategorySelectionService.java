// 記事投稿画面のカテゴリー選択プルダウン構築を担当するサービス。
// 親を持ち、かつ子を持たない末端カテゴリーのみをルートからのフルパス付きで返す。
package com.app.myblogpusher.service.Category;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.dto.Category.CategorySelectView;
import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.service.Article.ArticleCategoryService;

@Service
public class CategorySelectionService {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	/**
	 * 記事投稿画面のカテゴリー選択プルダウン用に、
	 * ルートからのフルパス付きでカテゴリー一覧を返す。
	 *
	 * 親カテゴリーを持つカテゴリーのうち、
	 * 子カテゴリーを持たない末端カテゴリーだけを選択肢にする。
	 */
	public List<CategoryOptionView> findSelectableCategories(Long userId) {

		List<ArticleCategory> categories = articleCategoryService.findByUserId(userId);

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
	 * 記事編集画面のカテゴリー選択用に、
	 * カテゴリーと親カテゴリーの関係を一括で返す。
	 *
	 * CategoryRelationを基準にすることで、
	 * 1つのカテゴリーが複数の親カテゴリーを持つ構造にも対応する。
	 *
	 * 階層の組み立てはJavaScript側で行うため、
	 * Java側ではカテゴリー自身と親カテゴリーの情報だけを返す。
	 */
	public List<CategorySelectView> findCategorySelects(Long userId) {

		List<ArticleCategory> categories = articleCategoryService.findByUserId(userId);

		if (categories.isEmpty()) {
			return List.of();
		}

		List<CategoryRelation> relations = categoryRelationRepository.findAll();

		return categories.stream()
				.map(category -> {

					List<Long> parentCategoryIds = relations.stream()
							.filter(relation -> category.getCategoryId()
									.equals(relation.getCategoryId()))
							.map(CategoryRelation::getParentCategoryId)
							.distinct()
							.toList();

					return new CategorySelectView(
							category.getCategoryId(),
							category.getCategoryName(),
							category.getDisplayName(),
							parentCategoryIds);
				})
				.toList();
	}
	
	/**
	 * 指定されたカテゴリーグループIDからカテゴリー経路を取得する。
	 *
	 * ArticleWorkが保持しているcategoryGroupIdは
	 * CategoryRelation.groupIdを指すため、そこから現在の
	 * カテゴリー経路を復元する。
	 */
	public String findCategoryPathByGroupId(Long groupId) {

		if (groupId == null) {
			return null;
		}

		return categoryRelationRepository
				.findByGroupId(groupId)
				.stream()
				.findFirst()
				.map(CategoryRelation::getCategoryPath)
				.orElse(null);
	}
}