
/**
 * 画像関連処理の窓口となるFacade
 *
 * 画像情報の取得はImageAssetQueryServiceへ、
 * 画像の登録・更新・削除・フォルダ名変更はImageAssetWriteServiceへ、
 * 既存画像の取り込みはImageAssetImportServiceへ委譲する。
 */
package com.app.myblogpusher.service.Facade;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.app.myblogpusher.dto.ImageAssetView;
import com.app.myblogpusher.dto.ImageCategoryDto;
import com.app.myblogpusher.entity.ImageAsset;
import com.app.myblogpusher.service.Image.ImageAssetImportService;
import com.app.myblogpusher.service.Image.ImageAssetQueryService;
import com.app.myblogpusher.service.Image.ImageAssetWriteService;

@Service
public class ImageAssetFacade {

	@Autowired
	private ImageAssetQueryService imageAssetQueryService;

	@Autowired
	private ImageAssetWriteService imageAssetWriteService;

	@Autowired
	private ImageAssetImportService imageAssetImportService;

	public ImageAsset uploadAndRegister(
			MultipartFile file,
			String folderName,
			Long userId) throws IOException {

		return imageAssetWriteService.uploadAndRegister(
				file, folderName, userId);
	}

	public List<String> listImageUrls(
			Long userId,
			String folderName) {

		return imageAssetQueryService.listImageUrls(
				userId, folderName);
	}

	public int importExistingImages(Long userId) {

		return imageAssetImportService.importExistingImages(userId);
	}

	public void updateImage(
			Long imageId,
			String folderName,
			String fileName,
			MultipartFile file,
			Long userId) throws IOException {

		imageAssetWriteService.updateImage(
				imageId, folderName, fileName, file, userId);
	}

	public void deleteImage(
			Long imageId,
			Long userId) {

		imageAssetWriteService.deleteImage(imageId, userId);
	}

	public List<ImageAssetView> listImages(Long userId, String folderName) {
		return imageAssetQueryService.listImages(userId, folderName);
	}

	/**
	 * ファイル名で絞り込んだ画像一覧を取得する。
	 */
	public List<ImageAssetView> listImages(
			Long userId,
			String folderName,
			String keyword) {
		return imageAssetQueryService.listImages(userId, folderName, keyword);
	}

	public Page<ImageAssetView> findImagePage(
			Long userId,
			String folderName,
			Pageable pageable) {

		return imageAssetQueryService.findImagePage(
				userId, folderName, pageable);
	}

	public List<ImageCategoryDto> findImageCategories(Long userId) {

		return imageAssetQueryService.findImageCategories(userId);
	}

	public List<String> findImageFolders(Long userId) {

		return imageAssetQueryService.findImageFolders(userId);
	}

	public void renameFolder(
			String oldFolderName,
			String newFolderName,
			Long userId) {

		imageAssetWriteService.renameFolder(
				oldFolderName, newFolderName, userId);
	}
}
