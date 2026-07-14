package com.app.myblogpusher.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}