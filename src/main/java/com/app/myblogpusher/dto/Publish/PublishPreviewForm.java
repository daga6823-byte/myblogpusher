/**
 * 投稿確認画面で使用する記事情報を保持するDTO
 *
 * 投稿確認画面への表示および投稿処理に必要な情報をまとめる。
 */

package com.app.myblogpusher.dto.Publish;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublishPreviewForm {

	private Long articleId;
	private Long categoryId;
	private String articleTitle;
	private String articleContent;
	private String slug;
	private String thumbnailUrl;
	private String repoOwner;
	private String repoName;
	
}