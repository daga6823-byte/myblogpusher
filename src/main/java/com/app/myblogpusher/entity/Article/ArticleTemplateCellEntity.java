
/**
 * 記事ごとのテンプレートセル情報を管理するエンティティ
 *
 * マスターテンプレートのセル構造を参照し、
 * 記事固有の変数値と画像割り当てを保持する。
 *
 * 編集中はwork_id、公開後はarticle_idで所有記事を識別する。
 * すべてのデータをユーザー単位で管理する。
 */
package com.app.myblogpusher.entity.Article;

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
@Table(name = "article_template_cell")
public class ArticleTemplateCellEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "work_id")
	private Long workId;

	@Column(name = "article_id")
	private Long articleId;

	@Column(name = "template_id", nullable = false)
	private Long templateId;

	@Column(name = "cell_id", nullable = false)
	private Long cellId;

	@Column(name = "variable_value")
	private String variableValue;

	@Column(name = "image_asset_id")
	private Long imageAssetId;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}
