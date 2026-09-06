/**
 * 記事内で使用する参考文献情報を管理するEntity
 *
 * カテゴリー経路単位で参考文献名とURLを保持する。
 * URLは書籍などURLを持たない資料にも対応するためNULL許容。
 */

package com.app.myblogpusher.entity.Article;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "article_reference")
@Data
public class ArticleReference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long referenceId;

	private Long userId;

	/**
	 * 参考文献を紐付けるカテゴリー経路
	 *
	 * CategoryRelation.groupIdを保持する。
	 * 例えば movie/batman 配下の記事で使用する参考文献なら、
	 * movie/batman を表すgroupIdを設定する。
	 */
	private Long groupId;

	private String referenceName;

	private String url;

	private LocalDateTime createDate;

	private LocalDateTime updateDate;
}