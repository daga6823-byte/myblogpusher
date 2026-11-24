/**
 * テンプレートの基本情報を管理するエンティティ
 *
 * ユーザーごとのテンプレート名、識別キー、説明、
 * テンプレート全体の背景色を保持する。
 */
package com.app.myblogpusher.entity.Template;

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
@Table(name = "template")
public class Template {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "template_id")
	private Long templateId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "template_name", nullable = false)
	private String templateName;

	@Column(name = "template_key", nullable = false)
	private String templateKey;

	@Column(name = "template_description")
	private String templateDescription;

	@Column(name = "background_color")
	private String backgroundColor;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}