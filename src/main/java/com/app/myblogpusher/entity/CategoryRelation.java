/**
 * カテゴリー同士の親子関係を表すエンティティ
 *
 * ArticleCategoryはカテゴリーそのものを表し、
 * このエンティティでカテゴリーがどの親カテゴリー配下に存在するかを管理する。
 *
 * 1つのカテゴリーを複数の親カテゴリー配下で再利用できる。
 */

package com.app.myblogpusher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category_relation")
public class CategoryRelation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "relation_id")
	private Long relationId;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(name = "parent_category_id", nullable = false)
	private Long parentCategoryId;
}