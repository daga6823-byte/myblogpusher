/**
 * ユーザーごとのテンプレート色履歴を管理するエンティティ
 *
 * テンプレート編集時に使用したカラーコードを保持し、
 * 過去に使用した色を再利用できるようにする。
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
@Table(name = "template_color")
public class TemplateColor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "color_id")
	private Long colorId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "color_code", nullable = false)
	private String colorCode;

	@Column(name = "create_date")
	private LocalDateTime createDate;
}