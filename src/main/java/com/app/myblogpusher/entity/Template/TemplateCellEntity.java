/**
 * テンプレートのセル情報を管理するエンティティ
 *
 * セルの位置・結合範囲・種類・内容・画像参照・装飾情報を保持する。
 */
package com.app.myblogpusher.entity.Template;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnTransformer;

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
@Table(name = "template_cell")
public class TemplateCellEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cell_id")
	private Long cellId;

	@Column(name = "template_id", nullable = false)
	private Long templateId;

	@Column(name = "row_number", nullable = false)
	private Integer rowNumber;

	@Column(name = "column_number", nullable = false)
	private Integer columnNumber;

	@Column(name = "row_span", nullable = false)
	private Integer rowSpan;

	@Column(name = "column_span", nullable = false)
	private Integer columnSpan;

	@Column(name = "cell_type", nullable = false)
	private String cellType;

	@Column(name = "cell_content")
	private String cellContent;

	@Column(name = "variable_name")
	private String variableName;

	@Column(name = "image_asset_id")
	private Long imageAssetId;

	@Column(name = "image_width")
	private BigDecimal imageWidth;

	@Column(name = "image_height")
	private BigDecimal imageHeight;

	@Column(name = "background_color")
	private String backgroundColor;

	@Column(name = "text_color")
	private String textColor;

	@Column(name = "font_size")
	private BigDecimal fontSize;

	@Column(name = "font_weight")
	private String fontWeight;

	@Column(name = "text_align")
	private String textAlign;

	@Column(name = "vertical_align")
	private String verticalAlign;

	@Column(name = "padding")
	private BigDecimal padding;

	@Column(name = "padding_unit")
	private String paddingUnit;

	@Column(name = "border_config")
	@ColumnTransformer(read = "border_config::text", write = "?::jsonb")
	private String borderConfig;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}