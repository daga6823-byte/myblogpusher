/**
 * カテゴリー同士の親子関係を管理するサービス
 *
 * ArticleCategoryはカテゴリーそのものを管理し、
 * CategoryRelationはカテゴリーがどの親カテゴリー配下に存在するかを管理する。
 */

package com.app.myblogpusher.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.repository.CategoryRelationRepository;

@Service
public class CategoryRelationService {

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	/**
	 * カテゴリーと親カテゴリーの関係を登録する。
	 */
	public void addRelation(Long categoryId, Long parentCategoryId) {

		if (parentCategoryId == null) {
			return;
		}

		CategoryRelation relation = new CategoryRelation();
		relation.setCategoryId(categoryId);
		relation.setParentCategoryId(parentCategoryId);

		categoryRelationRepository.save(relation);
	}

	/**
	 * 指定カテゴリーの親子関係をすべて削除する。
	 *
	 * カテゴリー更新時に、既存の親子関係を入れ替えるために使用する。
	 */
	public void deleteRelationsByCategoryId(Long categoryId) {

		List<CategoryRelation> relations = categoryRelationRepository
				.findByCategoryId(categoryId);

		categoryRelationRepository.deleteAll(relations);
	}

	/**
	 * 指定カテゴリーを親としている関係をすべて削除する。
	 *
	 * カテゴリー削除時に、子カテゴリー側に残る関係を削除するために使用する。
	 */
	public void deleteRelationsByParentCategoryId(Long parentCategoryId) {

		List<CategoryRelation> relations = categoryRelationRepository
				.findByParentCategoryId(parentCategoryId);

		categoryRelationRepository.deleteAll(relations);
	}
}