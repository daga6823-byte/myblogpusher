/**
 * 記事内で使用する参考文献情報を管理するEntity
 *
 * カテゴリー単位で参考文献名とURLを保持する。
 * URLは書籍などURLを持たない資料にも対応するためNULL許容。
 */

package com.app.myblogpusher.entity;

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

	private Long categoryId;

	private String referenceName;

	private String url;

	private LocalDateTime createDate;

	private LocalDateTime updateDate;
}