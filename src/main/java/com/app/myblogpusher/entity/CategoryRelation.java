/**
 * カテゴリー同士の親子関係とカテゴリー経路を管理するエンティティ
 *
 * ArticleCategoryはカテゴリーそのものを管理し、
 * CategoryRelationはカテゴリーがどの親カテゴリー配下に存在するか、
 * またその関係がどのカテゴリー経路に属するかを管理する。
 */

package com.app.myblogpusher.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

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

	@Column(name = "group_id")
	private Long groupId;

	@Column(name = "category_path", length = 1000)
	private String categoryPath;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;

	/**
	 * category_relationの複合主キーを表す。
	 *
	 * 現時点ではcategory_idとparent_category_idの組み合わせで
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