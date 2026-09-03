package com.app.myblogpusher.dto.Category;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CategoryDictionaryView {

	private final Long categoryId;
	private final String categoryName;
	private final List<Long> parentCategoryIds;
	private final String parentCategoryName;
	private final String displayName;
	private final long typoCount;

	public boolean isDeletable() {
		return typoCount == 0;
	}
}