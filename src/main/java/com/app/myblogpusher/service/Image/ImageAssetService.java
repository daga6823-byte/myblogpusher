
/**
 * 画像関連処理の互換性を維持する委譲サービス
 *
 * 既存の呼び出し元との互換性を保ち、
 * 画像関連処理をImageAssetFacadeへ委譲する。
 */
package com.app.myblogpusher.service.Image;

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
import com.app.myblogpusher.service.Facade.ImageAssetFacade;

@Service
public class ImageAssetService {

	@Autowired
	private ImageAssetFacade imageAssetFacade;

	public ImageAsset uploadAndRegister(
			MultipartFile file,
			String folderName,
			Long userId) throws IOException {

		return imageAssetFacade.uploadAndRegister(
				file, folderName, userId);
	}

	public List<String> listImageUrls(
			Long userId,
			String folderName) {

		return imageAssetFacade.listImageUrls(
				userId, folderName);
	}

	public int importExistingImages(Long userId) {

		return imageAssetFacade.importExistingImages(userId);
	}

	public void updateImage(
			Long imageId,
			String folderName,
			String fileName,
			MultipartFile file,
			Long userId) throws IOException {

		imageAssetFacade.updateImage(
				imageId, folderName, fileName, file, userId);
	}

	public void deleteImage(
			Long imageId,
			Long userId) {

		imageAssetFacade.deleteImage(imageId, userId);
	}

	public List<ImageAssetView> listImages(
			Long userId,
			String folderName) {

		return imageAssetFacade.listImages(userId, folderName);
	}

	public Page<ImageAssetView> findImagePage(
			Long userId,
			String folderName,
			Pageable pageable) {

		return imageAssetFacade.findImagePage(
				userId, folderName, pageable);
	}

	public List<ImageCategoryDto> findImageCategories(Long userId) {

		return imageAssetFacade.findImageCategories(userId);
	}

	public List<String> findImageFolders(Long userId) {

		return imageAssetFacade.findImageFolders(userId);
	}
}
