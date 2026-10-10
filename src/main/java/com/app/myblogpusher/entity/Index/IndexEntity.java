/**
 * Hugoカテゴリーの_index.mdを管理するエンティティ
 *
 * ユーザーごとにカテゴリー階層のインデックス内容を保持する。
 * groupIdでCategoryRelationのカテゴリー経路を特定し、
 * GitHub上の_index.mdとDBの内容を対応付ける。
 */

package com.app.myblogpusher.entity.Index;

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
@Table(name = "index")
public class IndexEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "index_id")
	private Long indexId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "title")
	private String title;

	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}
