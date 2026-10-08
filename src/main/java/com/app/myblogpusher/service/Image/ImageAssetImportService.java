/**
 * Storage上に存在する既存画像のインポートを担当するサービス
 *
 * Supabase Storage上の画像を走査し、
 * image_assetに未登録の画像をDBへ登録する。
 *
 * 過去にStorageへ直接アップロードされた画像を
 * image_assetへ取り込むための処理を担当する。
 */
package com.app.myblogpusher.service.Image;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ImageAsset;
import com.app.myblogpusher.repository.ImageAssetRepository;
import com.app.myblogpusher.service.SupabaseStorageService;

@Service
public class ImageAssetImportService {

	@Autowired
	private ImageAssetRepository imageAssetRepository;

	@Autowired
	private SupabaseStorageService supabaseStorageService;

	/**
	 * Supabase Storage上の実ファイルを走査し、
	 * まだimage_assetに登録されていない画像をDBへインポートする。
	 */
	public int importExistingImages(Long userId) {

		List<String> allPaths = supabaseStorageService.listAllFilePaths();

		Set<String> existingPaths = imageAssetRepository.findAll().stream()
				.map(ImageAsset::getStoragePath)
				.collect(Collectors.toSet());

		int importedCount = 0;

		for (String path : allPaths) {

			if (existingPaths.contains(path)) {
				continue;
			}

			int lastSlash = path.lastIndexOf('/');

			String folderName = lastSlash >= 0
					? path.substring(0, lastSlash)
					: "";

			String fileName = lastSlash >= 0
					? path.substring(lastSlash + 1)
					: path;

			ImageAsset asset = new ImageAsset();
			asset.setUserId(userId);
			asset.setFolderName(folderName);
			asset.setFileName(fileName);
			asset.setStoragePath(path);
			asset.setUploadDate(null);
			asset.setCreateUser(userId);
			asset.setUpdateUser(userId);
			asset.setCreateDate(LocalDateTime.now());
			asset.setUpdateDate(LocalDateTime.now());

			imageAssetRepository.save(asset);
			importedCount++;
		}

		return importedCount;
	}
}