
/**
 * インデックス編集中の一時保存データを管理するエンティティ
 */
package com.app.myblogpusher.entity.Index;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "index_workspace")
public class IndexWorkspace {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "group_id")
	private Long groupId;

	@Column(name = "title")
	private String title;

	@Column(name = "content")
	private String content;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}
