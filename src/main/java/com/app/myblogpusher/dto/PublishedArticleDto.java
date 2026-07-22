package com.app.myblogpusher.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

@Getter
public class PublishedArticleDto {
	
	private Long articleId;
	private String slug;
	private String hugoPath;
	private String title;
	private LocalDateTime updateDate;
	private String content;
	private List<String> categories;
	
	public PublishedArticleDto(
			Long articleId,
			String slug,
			String hugoPath,
			String title,
			LocalDateTime updateDate,
			String content,
			List<String> categories) {

		this.articleId = articleId;
		this.slug = slug;
		this.hugoPath = hugoPath;
		this.title = title;
		this.updateDate = updateDate;
		this.content = content;
		this.categories = categories;
	}
}
