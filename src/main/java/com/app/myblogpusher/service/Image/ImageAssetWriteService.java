/**
 * 画像情報の登録・更新・削除を担当するサービス
 *
 * Supabase Storage上の実ファイル操作と、
 * image_assetテーブルへの登録・更新・削除をまとめて行う。
 *
 * 画像情報の参照処理はImageAssetQueryServiceが担当する。
 */
package com.app.myblogpusher.service.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.app.myblogpusher.entity.ImageAsset;
import com.app.myblogpusher.repository.ImageAssetRepository;
import com.app.myblogpusher.service.SupabaseStorageService;

@Service
public class ImageAssetWriteService {

	@Autowired
	private ImageAssetRepository imageAssetRepository;

	@Autowired
	private SupabaseStorageService supabaseStorageService;

	@Autowired
	private ImageConvertService imageConvertService;

	@Autowired
	private ImageAssetCache imageAssetCache;

	/**
	 * 画像をアップロードし、image_assetに記録する。
	 *
	 * folderNameが未指定の場合はmiscフォルダを使用する。
	 */
	public ImageAsset uploadAndRegister(
			MultipartFile file,
			String folderName,
			Long userId) throws IOException {

		String resolvedFolderName;

		if (folderName != null && !folderName.isBlank()) {

			resolvedFolderName = folderName;

		} else {

			resolvedFolderName = "misc";
		}

		File convertedFile = imageConvertService.convert(file);

		String originalName = file.getOriginalFilename();

		if (originalName == null) {
			throw new IOException("ファイル名が取得できません");
		}

		String fileName = originalName;

		if (originalName.toLowerCase().endsWith(".heic")) {

			fileName = originalName.substring(
					0,
					originalName.lastIndexOf('.'))
					+ ".webp";
		}

		File uploadFile = new File(
				convertedFile.getParent(),
				fileName);

		Files.copy(
				convertedFile.toPath(),
				uploadFile.toPath(),
				java.nio.file.StandardCopyOption.REPLACE_EXISTING);

		String storagePath = supabaseStorageService.uploadImage(
				uploadFile,
				resolvedFolderName);

		ImageAsset asset = new ImageAsset();
		asset.setUserId(userId);
		asset.setFolderName(resolvedFolderName);
		asset.setFileName(fileName);
		asset.setStoragePath(storagePath);
		asset.setUploadDate(LocalDateTime.now());
		asset.setCreateUser(userId);
		asset.setUpdateUser(userId);
		asset.setCreateDate(LocalDateTime.now());
		asset.setUpdateDate(LocalDateTime.now());

		imageAssetRepository.save(asset);

		// 画像情報が変更されたため、次回表示時に最新情報を取得する。
		imageAssetCache.clear(userId);

		return asset;
	}

	/**
	 * 登録済み画像情報を更新する。
	 *
	 * 保存先フォルダ、画像名、画像ファイルの変更に対応する。
	 */
	public void updateImage(
			Long imageId,
			String folderName,
			String fileName,
			MultipartFile file,
			Long userId) throws IOException {

		ImageAsset asset = imageAssetRepository.findById(imageId)
				.orElseThrow();

		// 他ユーザーの画像を更新できないようにする。
		if (!asset.getUserId().equals(userId)) {
			throw new IllegalStateException(
					"他ユーザーの画像は更新できません");
		}

		String currentFolderName = asset.getFolderName();
		String currentFileName = asset.getFileName();

		String newFolderName = folderName;

		if (newFolderName == null || newFolderName.isBlank()) {
			throw new IllegalArgumentException(
					"保存先フォルダを指定してください");
		}

		String newFileName = fileName;

		if (newFileName == null || newFileName.isBlank()) {
			throw new IllegalArgumentException(
					"画像名を指定してください");
		}

		/*
		 * 新しい画像ファイルが指定された場合は、
		 * 入力された画像名をStorage上のファイル名として使用する。
		 *
		 * HEICの場合はアップロード時と同じくWebPへ変換する。
		 */
		if (file != null && !file.isEmpty()) {

			File convertedFile = imageConvertService.convert(file);

			String originalName = file.getOriginalFilename();

			if (originalName == null) {
				throw new IOException("ファイル名が取得できません");
			}

			if (originalName.toLowerCase().endsWith(".heic")) {

				newFileName = newFileName.substring(
						0,
						newFileName.lastIndexOf('.'))
						+ ".webp";
			}

			File uploadFile = new File(
					convertedFile.getParent(),
					newFileName);

			Files.copy(
					convertedFile.toPath(),
					uploadFile.toPath(),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);

			/*
			 * 保存先フォルダ・画像名をまとめて変更する場合は、
			 * 現在のStorage上のファイルを新しいパスへ移動する。
			 */
			if (!currentFolderName.equals(newFolderName)
					|| !currentFileName.equals(newFileName)) {

				String newStoragePath = supabaseStorageService.renameImage(
						asset.getStoragePath(),
						newFolderName,
						newFileName,
						uploadFile);

				asset.setFolderName(newFolderName);
				asset.setFileName(newFileName);
				asset.setStoragePath(newStoragePath);

			} else {

				// 保存先も画像名も同じ場合は実ファイルだけ差し替える。
				supabaseStorageService.replaceImage(
						asset.getStoragePath(),
						uploadFile);
			}

		} else if (!currentFolderName.equals(newFolderName)
				|| !currentFileName.equals(newFileName)) {

			/*
			 * ファイル差し替えなしで保存先または画像名だけ変更する。
			 */
			String newStoragePath = supabaseStorageService.renameImage(
					asset.getStoragePath(),
					newFolderName,
					newFileName,
					null);

			asset.setFolderName(newFolderName);
			asset.setFileName(newFileName);
			asset.setStoragePath(newStoragePath);
		}

		asset.setUpdateUser(userId);
		asset.setUpdateDate(LocalDateTime.now());

		imageAssetRepository.save(asset);

		// 画像変更により一覧・フォルダ情報が変わるため、
		// 次回表示時に最新情報を取得する。
		imageAssetCache.clear(userId);
	}

	/**
	 * 画像を削除する。
	 *
	 * image_assetの登録情報と、
	 * Supabase Storage上の実ファイルを削除する。
	 */
	public void deleteImage(
			Long imageId,
			Long userId) {

		ImageAsset asset = imageAssetRepository.findById(imageId)
				.orElseThrow();

		// 他ユーザーの画像削除防止
		if (!asset.getUserId().equals(userId)) {
			throw new IllegalStateException(
					"他ユーザーの画像は削除できません");
		}

		// Storage上の実ファイル削除
		supabaseStorageService.deleteImage(
				asset.getStoragePath());

		// DB削除
		imageAssetRepository.delete(asset);

		// 画像削除により一覧・カテゴリー・フォルダ情報が変わるため、
		// 次回表示時に最新情報を取得する。
		imageAssetCache.clear(userId);
	}

	/**
	 * フォルダ名を変更し、Storage上の実ファイルとDB登録情報を更新する。
	 */
	public void renameFolder(
			String oldFolderName,
			String newFolderName,
			Long userId) {

		if (oldFolderName == null || oldFolderName.isBlank()
				|| newFolderName == null || newFolderName.isBlank()) {
			throw new IllegalArgumentException(
					"変更前後のフォルダ名を指定してください");
		}

		if (oldFolderName.equals(newFolderName)) {
			throw new IllegalArgumentException(
					"変更前後のフォルダ名が同じです");
		}

		if (oldFolderName.contains("/") || newFolderName.contains("/")
				|| oldFolderName.equals(".") || newFolderName.equals(".")
				|| oldFolderName.equals("..") || newFolderName.equals("..")) {
			throw new IllegalArgumentException(
					"フォルダ名に使用できない文字が含まれています");
		}

		List<ImageAsset> assets = imageAssetRepository.findByUserIdAndFolderName(
				userId, oldFolderName);

		if (assets.isEmpty()) {
			throw new IllegalArgumentException(
					"変更対象の画像が見つかりません");
		}

		// Storageの実ファイルをフォルダ単位で移動する。
		Map<String, String> movedPaths = supabaseStorageService.renameFolder(
				oldFolderName, newFolderName);

		try {
			LocalDateTime now = LocalDateTime.now();

			for (ImageAsset asset : assets) {
				String newStoragePath = movedPaths.get(asset.getStoragePath());

				if (newStoragePath == null) {
					throw new IllegalStateException(
							"Storage上の画像パスとDB登録情報が一致しません");
				}

				asset.setFolderName(newFolderName);
				asset.setStoragePath(newStoragePath);
				asset.setUpdateUser(userId);
				asset.setUpdateDate(now);
			}

			imageAssetRepository.saveAll(assets);
			imageAssetCache.clear(userId);

		} catch (Exception e) {
			// DB更新に失敗した場合はStorageを元のフォルダへ戻す。
			try {
				supabaseStorageService.restoreMovedPaths(movedPaths);
			} catch (Exception rollbackException) {
				e.addSuppressed(rollbackException);
			}

			throw new RuntimeException(
					"フォルダ情報の更新に失敗しました", e);
		}
	}
}