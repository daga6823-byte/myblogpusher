/**
 * 記事編集画面のワークスペース情報を管理するエンティティ。
 *
 * 記事の編集中に入力内容を一時保存し、画面遷移や再ログイン後に
 * タイトル・本文・カテゴリーを復元できるようにする。
 * ユーザーごとに1件のワークスペースを保持する。
 */
package com.app.myblogpusher.entity.Article;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "article_workspace")
public class ArticleWorkspace {

	/** ワークスペースを所有するユーザーID（主キー）。 */
	@Id
	@Column(name = "user_id")
	private Long userId;

	/** 編集中の記事に選択されているカテゴリーグループID。 */
	@Column(name = "category_group_id")
	private Long categoryGroupId;

	/** 編集中の記事のタイトル。 */
	@Column(name = "title")
	private String title;

	/** 編集中の記事の本文（Markdown）。 */
	@Column(name = "content")
	private String content;

	/** ワークスペースの最終更新日時。 */
	@Column(name = "update_date")
	private LocalDateTime updateDate;

}
