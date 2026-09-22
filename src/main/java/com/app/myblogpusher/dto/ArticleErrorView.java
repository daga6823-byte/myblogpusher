/**
 * 投稿エラー確認画面に表示する記事情報を保持するView
 *
 * ArticleWorkの内部情報とエラーマスターの表示文を分離し、
 * 投稿エラー確認画面で必要な情報だけを保持する。
 */

package com.app.myblogpusher.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleErrorView {

	private Long workId;

	private String title;

	private String errorMessage;

	public ArticleErrorView(
			Long workId,
			String title,
			String errorMessage) {

		this.workId = workId;
		this.title = title;
		this.errorMessage = errorMessage;
	}
}