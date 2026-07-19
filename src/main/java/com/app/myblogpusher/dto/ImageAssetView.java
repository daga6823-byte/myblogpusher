/**
 * 登録済み画像の確認画面に表示するためのビューDTO
 */

package com.app.myblogpusher.dto;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class ImageAssetView {

	private final Long imageId;
	private final Long categoryId;
	private final String folderName;
	private final String fileName;
	private final String categoryName;
	private final LocalDateTime uploadDate;
	private final String url;

	public ImageAssetView(
			Long imageId,
			Long categoryId,
			String folderName,
			String fileName,
			String categoryName,
			LocalDateTime uploadDate,
			String url) {
		this.imageId = imageId;
		this.categoryId = categoryId;
		this.folderName = folderName;
		this.fileName = fileName;
		this.categoryName = categoryName;
		this.uploadDate = uploadDate;
		this.url = url;
	}
}