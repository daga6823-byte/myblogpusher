/**
 * 画像カテゴリー選択用DTO
 *
 * image_assetに登録されているフォルダ一覧を
 * 画像選択画面のカテゴリーコンボボックスへ表示する。
 */

package com.app.myblogpusher.dto;

public class ImageCategoryDto {

	private Long categoryId;

	private String folderName;

	public ImageCategoryDto(
			Long categoryId,
			String folderName) {

		this.categoryId = categoryId;
		this.folderName = folderName;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getFolderName() {
		return folderName;
	}
}