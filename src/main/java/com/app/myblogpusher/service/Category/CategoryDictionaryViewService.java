// カテゴリー辞典画面向けの表示用データ構築を担当するサービス。
// 誤字登録件数の集計、カテゴリー削除可否の判定（誤字が紐づく場合は削除不可）を持つ。
package com.app.myblogpusher.service.Category;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Category.CategoryDictionaryView;
import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.TypoCorrectionRepository;
import com.app.myblogpusher.service.Article.ArticleCategoryService;

@Service
public class CategoryDictionaryViewService {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	@Autowired
	private TypoCorrectionRepository typoCorrectionRepository;

	/**
	 * カテゴリー辞典表示用の一覧を取得する
	 *
	 * 誤字登録件数と親カテゴリー表示名を付加して返す。
	 */
	public List<CategoryDictionaryView> findDictionaryView(Long userId) {

		List<ArticleCategory> categories = articleCategoryService.findByUserId(userId);

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
	 * カテゴリーが誤字補正に使用中かどうかを判定する。
	 *
	 * 使用中の場合は削除不可。
	 */
	public boolean isInUse(Long categoryId) {
		long typoCount = typoCorrectionRepository.countByCategoryIds(List.of(categoryId))
				.stream()
				.findFirst()
				.map(row -> (Long) row[1])
				.orElse(0L);
		return typoCount > 0;
	}
}