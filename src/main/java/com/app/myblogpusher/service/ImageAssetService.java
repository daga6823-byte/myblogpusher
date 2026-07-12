/**
 * 画像アップロードの調整役サービス
 * Supabase Storageへの実アップロードとimage_assetテーブルへの記録をあわせて行う
 * フォルダ名はカテゴリーのスラッグをデフォルトとし、アップロード時に上書き指定できる
 */

package com.app.myblogpusher.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

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
}