package com.app.myblogpusher.dto;

import java.time.LocalDateTime;

public class PublishedArticleSummaryDto {
	private String slug;
	private String title;
	private LocalDateTime updateDate;

	public PublishedArticleSummaryDto(String slug, String title, LocalDateTime updateDate) {
		this.slug = slug;
		this.title = title;
		this.updateDate = updateDate;
	}

	public String getSlug() {
		return slug;
	}

	public String getTitle() {
		return title;
	}

	public LocalDateTime getUpdateDate() {
		return updateDate;
	}
}