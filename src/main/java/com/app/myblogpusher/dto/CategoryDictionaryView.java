package com.app.myblogpusher.dto;

public class CategoryDictionaryView {

	private final Long categoryId;
	private final String categoryName;
	private final Long parentCategoryId;
	private final String displayName;
	private final long typoCount;

	public CategoryDictionaryView(Long categoryId, String categoryName, long parentCategoryId,
			String displayName, long typoCount) {
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.parentCategoryId = parentCategoryId;
		this.displayName = displayName;
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

	public long getParentCategoryId() {
		return parentCategoryId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isDeletable() {
		return typoCount == 0;
	}

}