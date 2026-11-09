/**
 * 記事編集画面のカテゴリー階層選択用ビューDTO
 *
 * CategoryRelationの情報を基準として、
 * カテゴリーID、親カテゴリーID、groupId、カテゴリー経路を保持する。
 */
package com.app.myblogpusher.dto.Category;

public class CategorySelectView {

	private final Long categoryId;
	private final String categoryName;
	private final String displayName;
	private final Long parentCategoryId;
	private final Long groupId;
	private final String categoryPath;

	public CategorySelectView(
			Long categoryId,
			String categoryName,
			String displayName,
			Long parentCategoryId,
			Long groupId,
			String categoryPath) {

		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.displayName = displayName;
		this.parentCategoryId = parentCategoryId;
		this.groupId = groupId;
		this.categoryPath = categoryPath;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Long getParentCategoryId() {
		return parentCategoryId;
	}

	public Long getGroupId() {
		return groupId;
	}

	public String getCategoryPath() {
		return categoryPath;
	}
}