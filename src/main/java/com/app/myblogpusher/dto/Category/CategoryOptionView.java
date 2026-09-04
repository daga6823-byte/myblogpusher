/**
 * 記事投稿画面のカテゴリー選択プルダウン用のビューDTO
 * ルートから自分自身までの表示名（displayName優先、無ければcategoryName）を
 * "/"で連結したフルパスを持つ（例: 映画/バットマン/バットマン(1989)）
 */

package com.app.myblogpusher.dto.Category;

public class CategoryOptionView {

	private final Long groupId;
	private final Long categoryId;
	private final String categoryPath;

	public CategoryOptionView(
			Long groupId,
			Long categoryId,
			String categoryPath) {

		this.groupId = groupId;
		this.categoryId = categoryId;
		this.categoryPath = categoryPath;
	}

	public Long getGroupId() {
		return groupId;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getCategoryPath() {
		return categoryPath;
	}
}