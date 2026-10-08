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
import com.app.myblogpusher.service.Image.ImageAssetService;
import com.app.myblogpusher.service.Image.ImageAssetWriteService;

/**
 * 画像管理処理のファサード
 *
 * Controllerから画像管理に関する処理を呼び出す際の窓口となる。
 *
 * 画像の登録・更新・削除・取得・インポートなどの実処理は
 * 各専門Serviceへ委譲し、Controllerから複数のServiceを
 * 直接参照しない構成にする。
 *
 * サービス分割の移行期間中は既存のImageAssetServiceも利用し、
 * 処理単位ごとの分離完了後に不要な委譲処理を削除する。
 */
@Service
public class ImageAssetFacadeService {

	@Autowired
	private ImageAssetService imageAssetService;

	@Autowired
	private ImageAssetQueryService imageAssetQueryService;

	@Autowired
	private ImageAssetWriteService imageAssetWriteService;

	@Autowired
	private ImageAssetImportService imageAssetImportService;

	/**
	 * 画像をアップロードし、image_assetに記録する。
	 */
	public ImageAsset uploadAndRegister(
			MultipartFile file,
			String folderName,
			Long userId) throws IOException {

		return imageAssetWriteService.uploadAndRegister(
				file,
				folderName,
				userId);
	}

	/**
	 * DBに記録された画像一覧からURLリストを取得する。
	 */
	public List<String> listImageUrls(
			Long userId,
			String folderName) {

		return imageAssetQueryService.listImageUrls(
				userId,
				folderName);
	}

	/**
	 * Supabase Storage上の既存画像をDBへインポートする。
	 */
	public int importExistingImages(Long userId) {

		return imageAssetImportService.importExistingImages(userId);
	}

	/**
	 * 登録済み画像情報を更新する。
	 */
	public void updateImage(
			Long imageId,
			String folderName,
			String fileName,
			MultipartFile file,
			Long userId) throws IOException {

		imageAssetWriteService.updateImage(
				imageId,
				folderName,
				fileName,
				file,
				userId);
	}

	/**
	 * 画像を削除する。
	 */
	public void deleteImage(
			Long imageId,
			Long userId) {

		imageAssetWriteService.deleteImage(
				imageId,
				userId);
	}

	/**
	 * 画像一覧を取得する。
	 */
	public List<ImageAssetView> listImages(
			Long userId,
			String folderName) {

		return imageAssetQueryService.listImages(
				userId,
				folderName);
	}

	/**
	 * 画像一覧をページングして取得する。
	 */
	public Page<ImageAssetView> findImagePage(
			Long userId,
			String folderName,
			Pageable pageable) {

		return imageAssetQueryService.findImagePage(
				userId,
				folderName,
				pageable);
	}

	/**
	 * 画像保存先フォルダ一覧を取得する。
	 */
	public List<ImageCategoryDto> findImageCategories(
			Long userId) {

		return imageAssetQueryService.findImageCategories(
				userId);
	}

	/**
	 * 画像保存先フォルダ名一覧を取得する。
	 */
	public List<String> findImageFolders(
			Long userId) {

		return imageAssetQueryService.findImageFolders(
				userId);
	}
}