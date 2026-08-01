/**
 * リンク挿入モーダル用の記事一覧DTO
 *
 * 記事リンク作成に必要な最小限の情報のみ保持する。
 */

package com.app.myblogpusher.dto.Article;

import lombok.Getter;
@Getter
public class ArticleLinkView {
		private String slug;
		private String hugoPath;
		private String title;

		public ArticleLinkView(
				String slug,
				String hugoPath,
				String title) {

			this.slug = slug;
			this.hugoPath = hugoPath;
			this.title = title;
		}
}
