/**
 * 登録済み画像の確認画面に表示するためのビューDTO
 */

package com.app.myblogpusher.dto;

import java.time.LocalDateTime;

public class ImageAssetView {

	private final Long imageId;
	private final String folderName;
	private final String fileName;
	private final String categoryName;
	private final LocalDateTime uploadDate;
	private final String url;

	public ImageAssetView(Long imageId, String folderName, String fileName, String categoryName,
			LocalDateTime uploadDate, String url) {
		this.imageId = imageId;
		this.folderName = folderName;
		this.fileName = fileName;
		this.categoryName = categoryName;
		this.uploadDate = uploadDate;
		this.url = url;
	}

	public Long getImageId() {
		return imageId;
	}

	public String getFolderName() {
		return folderName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public LocalDateTime getUploadDate() {
		return uploadDate;
	}

	public String getUrl() {
		return url;
	}
}