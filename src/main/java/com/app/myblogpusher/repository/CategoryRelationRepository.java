/**
 * カテゴリー同士の親子関係の永続化を担当するリポジトリ
 */

package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.CategoryRelation;

public interface CategoryRelationRepository extends JpaRepository<CategoryRelation, Long> {

	List<CategoryRelation> findByCategoryId(Long categoryId);

	List<CategoryRelation> findByParentCategoryId(Long parentCategoryId);
}