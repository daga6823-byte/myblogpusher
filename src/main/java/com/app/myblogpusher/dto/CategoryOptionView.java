/**
 * 記事投稿画面のカテゴリー選択プルダウン用のビューDTO
 * ルートから自分自身までの表示名（displayName優先、無ければcategoryName）を
 * "/"で連結したフルパスを持つ（例: 映画/バットマン/バットマン(1989)）
 */

package com.app.myblogpusher.dto;

public class CategoryOptionView {

	private final Long categoryId;
	private final String fullPath;

	public CategoryOptionView(Long categoryId, String fullPath) {
		this.categoryId = categoryId;
		this.fullPath = fullPath;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getFullPath() {
		return fullPath;
	}
}