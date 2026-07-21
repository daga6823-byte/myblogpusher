package com.app.myblogpusher.dto;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class PublishedArticleSummaryDto {
	private String slug;
	private String hugoPath;
	private String title;
	private LocalDateTime updateDate;

	public PublishedArticleSummaryDto(
			String slug,
			String hugoPath,
			String title,
			LocalDateTime updateDate) {
		this.slug = slug;
		this.hugoPath = hugoPath;
		this.title = title;
		this.updateDate = updateDate;
	}
}