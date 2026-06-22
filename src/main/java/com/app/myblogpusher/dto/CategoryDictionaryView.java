package com.app.myblogpusher.dto;

public class CategoryDictionaryView {

	private final Long categoryId;
	private final String categoryName;
	private final long typoCount;

	public CategoryDictionaryView(Long categoryId, String categoryName, long typoCount) {
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.typoCount = typoCount;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public long getTypoCount() {
		return typoCount;
	}

	public boolean isDeletable() {
		return typoCount == 0;
	}
}