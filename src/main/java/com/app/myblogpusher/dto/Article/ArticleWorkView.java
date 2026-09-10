package com.app.myblogpusher.dto.Article;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleWorkView {

	private final Long workId;
	private final String title;
	private final Long categoryGroupId;
	private final String categoryName;
	private final LocalDateTime updateDate;

	public ArticleWorkView(
			Long workId,
			String title,
			Long categoryGroupId,
			String categoryName,
			LocalDateTime updateDate) {

		this.workId = workId;
		this.title = title;
		this.categoryGroupId = categoryGroupId;
		this.categoryName = categoryName;
		this.updateDate = updateDate;
	}
}