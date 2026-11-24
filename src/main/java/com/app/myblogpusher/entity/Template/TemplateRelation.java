/**
 * テンプレート同士の親子関係を管理するエンティティ
 *
 * 親テンプレートと子テンプレートの関連情報を保持する。
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
@Table(name = "template_relation")
public class TemplateRelation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "relation_id")
	private Long relationId;

	@Column(name = "parent_template_id", nullable = false)
	private Long parentTemplateId;

	@Column(name = "child_template_id", nullable = false)
	private Long childTemplateId;

	@Column(name = "create_date")
	private LocalDateTime createDate;
}