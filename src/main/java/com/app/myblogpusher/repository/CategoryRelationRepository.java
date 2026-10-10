/**
 * カテゴリー階層の関連情報を取得・管理するリポジトリ
 *
 * カテゴリーID、親カテゴリーID、グループID、カテゴリー経路を
 * 基準として、CategoryRelationの検索を担当する。
 */
package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.CategoryRelation;

public interface CategoryRelationRepository
		extends JpaRepository<CategoryRelation, Long> {

	/**
	 * 指定したカテゴリーIDに紐づく階層情報を取得する。
	 */
	List<CategoryRelation> findByCategoryId(Long categoryId);

	/**
	 * 指定した親カテゴリーIDを持つ階層情報を取得する。
	 */
	List<CategoryRelation> findByParentCategoryId(Long parentCategoryId);

	/**
	 * 指定したカテゴリー経路に一致する階層情報を取得する。
	 */
	List<CategoryRelation> findByCategoryPath(String categoryPath);

	/**
	 * 指定したグループIDに紐づく階層情報を取得する。
	 */
	List<CategoryRelation> findByGroupId(Long groupId);

	/**
	 * 指定したカテゴリーID・親カテゴリーID・カテゴリー経路の組み合わせが
	 * すでに登録されているか確認する。
	 */
	boolean existsByCategoryIdAndParentCategoryIdAndCategoryPath(
			Long categoryId,
			Long parentCategoryId,
			String categoryPath);

	/**
	 * 指定した複数のグループIDに紐づく階層情報を一括取得する。
	 */
	List<CategoryRelation> findByGroupIdIn(List<Long> groupIds);
}
