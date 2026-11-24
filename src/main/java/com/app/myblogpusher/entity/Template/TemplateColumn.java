/**
 * テンプレートの列情報を管理するエンティティ
 *
 * テンプレート内の列番号と列の幅・単位を保持する。
 */
package com.app.myblogpusher.entity.Template;

import java.math.BigDecimal;
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
@Table(name = "template_column")
public class TemplateColumn {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "column_id")
	private Long columnId;

	@Column(name = "template_id", nullable = false)
	private Long templateId;

	@Column(name = "column_number", nullable = false)
	private Integer columnNumber;

	@Column(name = "width")
	private BigDecimal width;

	@Column(name = "width_unit", nullable = false)
	private String widthUnit;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}