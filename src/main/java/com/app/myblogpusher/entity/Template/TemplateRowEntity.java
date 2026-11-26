/**
 * テンプレートの行情報を管理するエンティティ
 *
 * テンプレート内の行番号と行の高さ・単位を保持する。
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
@Table(name = "template_row")
public class TemplateRowEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "row_id")
	private Long rowId;

	@Column(name = "template_id", nullable = false)
	private Long templateId;

	@Column(name = "row_number", nullable = false)
	private Integer rowNumber;

	@Column(name = "height")
	private BigDecimal height;

	@Column(name = "height_unit", nullable = false)
	private String heightUnit;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}