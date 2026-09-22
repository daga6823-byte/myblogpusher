/**
 * 投稿処理などで発生するエラーのマスター情報を管理するエンティティ
 *
 * エラーコードとユーザー向け表示文を紐付け、
 * ArticleWorkにはエラーコードのみを保持する。
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
@Table(name = "error_master")
public class ErrorMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "error_id")
	private Long errorId;

	@Column(name = "error_code", nullable = false, unique = true, length = 100)
	private String errorCode;

	@Column(name = "error_name", nullable = false, length = 200)
	private String errorName;

	@Column(name = "error_message", nullable = false, length = 1000)
	private String errorMessage;

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "enabled", nullable = false)
	private Boolean enabled;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;
}