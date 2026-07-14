package com.app.myblogpusher.dto;

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
	private String repoOwner;
	private String repoName;
}