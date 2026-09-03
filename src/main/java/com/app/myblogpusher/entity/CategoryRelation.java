/**
 * カテゴリー同士の親子関係を表すエンティティ
 *
 * ArticleCategoryはカテゴリーそのものを表し、
 * このエンティティでカテゴリーがどの親カテゴリー配下に存在するかを管理する。
 *
 * 1つのカテゴリーを複数の親カテゴリー配下で再利用できる。
 */

package com.app.myblogpusher.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@IdClass(CategoryRelation.CategoryRelationId.class)
@Table(name = "category_relation")
public class CategoryRelation {

	@Id
	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Id
	@Column(name = "parent_category_id", nullable = false)
	private Long parentCategoryId;

	/**
	 * category_relationの複合主キーを表す。
	 *
	 * category_idとparent_category_idの組み合わせで
	 * 1つの親子関係を識別する。
	 */
	@Getter
	@Setter
	public static class CategoryRelationId implements Serializable {

		private static final long serialVersionUID = 1L;

		private Long categoryId;
		private Long parentCategoryId;
	}
}