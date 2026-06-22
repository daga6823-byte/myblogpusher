package com.app.myblogpusher.dto;

import java.time.LocalDateTime;

public class ArticleWorkView {

	private final Long workId;
	private final String title;
	private final String categoryName;
	private final LocalDateTime updateDate;

	public ArticleWorkView(Long workId, String title, String categoryName, LocalDateTime updateDate) {
		this.workId = workId;
		this.title = title;
		this.categoryName = categoryName;
		this.updateDate = updateDate;
	}

	public Long getWorkId() {
		return workId;
	}

	public String getTitle() {
		return title;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public LocalDateTime getUpdateDate() {
		return updateDate;
	}
}