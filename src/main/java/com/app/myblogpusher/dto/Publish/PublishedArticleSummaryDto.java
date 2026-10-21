/**
 * 投稿済み記事一覧の表示データを保持するDTO
 *
 * 記事一覧画面で使用するスラッグ、Hugoパス、タイトル、
 * 更新日時、本文、カテゴリー情報を保持する。
 * 記事の取得やデータ加工などの処理は担当しない。
 */
package com.app.myblogpusher.dto.Publish;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

@Getter
public class PublishedArticleSummaryDto {
	private String slug;
	private String hugoPath;
	private String title;
	private LocalDateTime updateDate;
	private String content;
	private List<String> categories;

	public PublishedArticleSummaryDto(
			String slug,
			String hugoPath,
			String title,
			LocalDateTime updateDate,
			String content,
			List<String> categories) {

		this.slug = slug;
		this.hugoPath = hugoPath;
		this.title = title;
		this.updateDate = updateDate;
		this.content = content;
		this.categories = categories;
	}
}
