/**
 * 記事リンク挿入モーダル用の記事一覧DTO
 *
 * 記事リンク作成に必要な記事情報とカテゴリーグループIDを保持する。
 */
package com.app.myblogpusher.dto.Article;

import lombok.Getter;

@Getter
public class ArticleLinkView {

	private String slug;

	private String hugoPath;

	private String title;

	private String url;

	private Long groupId;

	public ArticleLinkView(
			String slug,
			String hugoPath,
			String title,
			String url,
			Long groupId) {

		this.slug = slug;
		this.hugoPath = hugoPath;
		this.title = title;
		this.url = url;
		this.groupId = groupId;
	}
}