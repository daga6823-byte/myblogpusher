/**
 * Hugoカテゴリーのインデックス編集作業を管理するエンティティ
 *
 * GitHub反映前の編集内容と処理状態を保持する。
 * GitHub反映に失敗した場合も作業内容を残し、再試行できるようにする。
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
@Table(name = "index_work")
public class IndexWork {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "work_id")
	private Long workId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "group_id")
	private Long groupId;

	@Column(name = "title")
	private String title;

	@Column(name = "content")
	private String content;

	@Column(name = "status")
	private Integer status;

	@Column(name = "error_code")
	private String errorCode;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;
}