/**
 * 記事編集画面のカテゴリー階層選択用ビューDTO
 *
 * カテゴリー自身のIDと表示名、親カテゴリーIDを保持する。
 * 記事編集画面ではカテゴリーを階層ごとに選択するため、
 * CategoryOptionViewのような完成済みcategoryPathは使用しない。
 */

package com.app.myblogpusher.dto.Category;

import java.util.List;

public class CategorySelectView {

	private final Long categoryId;
	private final String categoryName;
	private final String displayName;
	private final List<Long> parentCategoryIds;

	public CategorySelectView(
	        Long categoryId,
	        String categoryName,
	        String displayName,
	        List<Long> parentCategoryIds) {

	    this.categoryId = categoryId;
	    this.categoryName = categoryName;
	    this.displayName = displayName;
	    this.parentCategoryIds = parentCategoryIds;
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

	public List<Long> getParentCategoryIds() {
	    return parentCategoryIds;
	}
}