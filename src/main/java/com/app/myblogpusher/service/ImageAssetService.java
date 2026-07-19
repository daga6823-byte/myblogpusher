/**
 * 画像アップロードの調整役サービス
 * Supabase Storageへの実アップロードとimage_assetテーブルへの記録をあわせて行う
 * フォルダ名はカテゴリーのスラッグをデフォルトとし、アップロード時に上書き指定できる
 */

package com.app.myblogpusher.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ImageAsset;
import com.app.myblogpusher.repository.ImageAssetRepository;
import com.app.myblogpusher.util.SlugUtil;

@Service
public class ImageAssetService {

	@Autowired
	private ImageAssetRepository imageAssetRepository;

	@Autowired
	private SupabaseStorageService supabaseStorageService;

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private SlugUtil slugUtil;

	/**
	 * カテゴリーからデフォルトのフォルダ名（スラッグ）を求める
	 */
	public String resolveDefaultFolderName(Long categoryId) {
		return articleCategoryService.findById(categoryId)
				.map(ArticleCategory::getCategoryName)
				.map(slugUtil::generateCategorySlug)
				.orElse("misc");
	}

	/**
	 * 画像をアップロードし、image_assetに記録する
	 * folderNameが未指定ならcategoryIdから求めたデフォルトフォルダを使う
	 */
	public ImageAsset uploadAndRegister(
			MultipartFile file,
			String folderName,
			Long categoryId,
			Long workId,
			Long userId) throws IOException {

		String resolvedFolderName = (folderName != null && !folderName.isBlank())
				? folderName
				: resolveDefaultFolderName(categoryId);

		String storagePath = supabaseStorageService.uploadImage(file, resolvedFolderName);

		ImageAsset asset = new ImageAsset();
		asset.setUserId(userId);
		asset.setCategoryId(categoryId);
		asset.setWorkId(workId);
		asset.setFolderName(resolvedFolderName);
		asset.setFileName(file.getOriginalFilename());
		asset.setStoragePath(storagePath);
		asset.setUploadDate(LocalDateTime.now());
		asset.setCreateUser(userId);
		asset.setUpdateUser(userId);
		asset.setCreateDate(LocalDateTime.now());
		asset.setUpdateDate(LocalDateTime.now());

		imageAssetRepository.save(asset);
		return asset;
	}

	/**
	 * DBに記録された画像一覧からURLリストを組み立てる（Supabase側の一覧APIには依存しない）
	 * categoryIdが指定されればそのカテゴリー分だけに絞り込む
	 */
	public List<String> listImageUrls(Long userId, Long categoryId) {
		List<ImageAsset> assets = (categoryId != null)
				? imageAssetRepository.findByUserIdAndCategoryIdOrderByUploadDateDesc(userId, categoryId)
				: imageAssetRepository.findByUserIdOrderByUploadDateDesc(userId);

		return assets.stream()
				.map(a -> supabaseStorageService.getImageUrl(a.getStoragePath()))
				.toList();
	}

	/**
	 * Supabase Storage上の実ファイルを走査し、まだimage_assetに登録されていない
	 * 画像をDBにインポートする（過去に手動アップロードした画像の救済用）
	 * フォルダ名がカテゴリーのスラッグと一致すればcategoryIdを推定して設定する
	 */
	public int importExistingImages(Long userId) {
		List<String> allPaths = supabaseStorageService.listAllFilePaths();

		Set<String> existingPaths = imageAssetRepository.findAll().stream()
				.map(ImageAsset::getStoragePath)
				.collect(Collectors.toSet());

		Map<String, Long> slugToCategoryId = articleCategoryService.findByUserId(userId).stream()
				.collect(Collectors.toMap(
						c -> slugUtil.generateCategorySlug(c.getCategoryName()),
						ArticleCategory::getCategoryId,
						(a, b) -> a));

		int importedCount = 0;

		for (String path : allPaths) {
			if (existingPaths.contains(path)) {
				continue;
			}

			int lastSlash = path.lastIndexOf('/');
			String folderName = lastSlash >= 0 ? path.substring(0, lastSlash) : "";
			String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

			Long matchedCategoryId = slugToCategoryId.get(folderName);

			ImageAsset asset = new ImageAsset();
			asset.setUserId(userId);
			asset.setCategoryId(matchedCategoryId);
			asset.setWorkId(null);
			asset.setFolderName(folderName);
			asset.setFileName(fileName);
			asset.setStoragePath(path);
			asset.setUploadDate(null); // 実際のアップロード日時は不明のため未設定
			asset.setCreateUser(userId);
			asset.setUpdateUser(userId);
			asset.setCreateDate(LocalDateTime.now());
			asset.setUpdateDate(LocalDateTime.now());

			imageAssetRepository.save(asset);
			importedCount++;
		}

		return importedCount;
	}

	/**
	 * 登録済み画像情報を更新する
	 *
	 * ・カテゴリー変更
	 * ・保存先フォルダ変更
	 */
	/**
	 * 登録済み画像情報を更新する
	 *
	 * ・カテゴリー変更
	 * ・保存先フォルダ変更
	 */
	public void updateImage(
			Long imageId,
			Long categoryId,
			String folderName,
			Long userId) {

		ImageAsset asset = imageAssetRepository.findById(imageId)
				.orElseThrow();

		// フォルダが変更された場合のみStorage上も移動する
		if (!folderName.equals(asset.getFolderName())) {

			String newStoragePath = supabaseStorageService.moveImage(
					asset.getStoragePath(),
					folderName);

			asset.setFolderName(folderName);
			asset.setStoragePath(newStoragePath);
		}

		asset.setCategoryId(categoryId);
		asset.setUpdateUser(userId);
		asset.setUpdateDate(LocalDateTime.now());

		imageAssetRepository.save(asset);

	}
}