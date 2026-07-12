/**
 * カテゴリー（movie/note/drama/techなどのメインカテゴリー、およびそのサブカテゴリー）を表すエンティティ
 * parentCategoryIdによる自己参照で親子階層を表現する（NULLならメインカテゴリー）
 * displayNameはHugoの_index.mdのtitleに使う日本語表示名
 * sortOrderは同じ親を持つカテゴリー同士の表示順（小さいほど上に表示）
 */

package com.app.myblogpusher.entity;

import java.time.LocalDateTime;

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
@Table(name = "article_category")
public class ArticleCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "category_id")
	private Long categoryId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "category_name", nullable = false)
	private String categoryName;

	@Column(name = "parent_category_id")
	private Long parentCategoryId;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "sort_order")
	private Integer sortOrder = 0;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}