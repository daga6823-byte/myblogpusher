/**
 * 記事内で使用する参考文献情報を管理するEntity
 *
 * カテゴリーそのものに対して参考文献を紐付ける。
 * そのため、movie/batman、comic/batmanなど複数のカテゴリー経路から
 * 同じBatmanカテゴリーの参考文献を共有できる。
 *
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
	 * 参考文献を紐付けるカテゴリー
	 *
	 * ArticleCategory.categoryIdを保持する。
	 * 複数のカテゴリー経路で使用される同一カテゴリーの
	 * 参考文献を共有する。
	 */
	private Long categoryId;

	private String referenceName;

	private String url;

	private LocalDateTime createDate;

	private LocalDateTime updateDate;
}