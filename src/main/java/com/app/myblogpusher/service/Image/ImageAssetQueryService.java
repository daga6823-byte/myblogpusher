/**
 * 画像情報の取得を担当するサービス
 *
 * image_assetテーブルから画像一覧・ページング・
 * 保存先フォルダ情報を取得する。
 *
 * 画像情報の取得時は、必要に応じてキャッシュを利用する。
 */
package com.app.myblogpusher.service.Image;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.ImageAssetView;
import com.app.myblogpusher.dto.ImageCategoryDto;
import com.app.myblogpusher.entity.ImageAsset;
import com.app.myblogpusher.repository.ImageAssetRepository;
import com.app.myblogpusher.service.SupabaseStorageService;

@Service
public class ImageAssetQueryService {

	@Autowired
	private ImageAssetRepository imageAssetRepository;

	@Autowired
	private SupabaseStorageService supabaseStorageService;

	@Autowired
	private ImageAssetCache imageAssetCache;

	/**
	 * DBに記録された画像一覧からURLリストを組み立てる。
	 *
	 * Supabase側の一覧APIには依存せず、
	 * image_assetに登録された情報を使用する。
	 */
	public List<String> listImageUrls(Long userId, String folderName) {

		List<ImageAsset> assets;

		if (folderName != null && !folderName.isBlank()) {

			assets = imageAssetRepository
					.findByUserIdAndFolderNameOrderByUploadDateDesc(
							userId,
							folderName,
							org.springframework.data.domain.Pageable.unpaged())
					.getContent();

		} else {

			assets = imageAssetRepository
					.findByUserIdOrderByUploadDateDesc(userId);
		}

		return assets.stream()
				.map(a -> supabaseStorageService.getImageUrl(
						a.getStoragePath()))
				.toList();
	}

	/**
	 * 画像一覧を取得する。
	 *
	 * 全カテゴリーの場合はログイン時に先読みした
	 * キャッシュを利用する。
	 */
	public List<ImageAssetView> listImages(
			Long userId,
			String folderName) {

		if (folderName == null) {

			List<ImageAssetView> cachedImages =
					imageAssetCache.getImages(userId);

			if (cachedImages != null) {
				return cachedImages;
			}
		}

		List<ImageAsset> assets = (folderName != null)
				? imageAssetRepository
						.findByUserIdAndFolderNameOrderByUploadDateDesc(
								userId,
								folderName,
								org.springframework.data.domain.Pageable.unpaged())
						.getContent()
				: imageAssetRepository
						.findByUserIdOrderByUploadDateDesc(userId);

		return assets.stream()
				.map(asset -> {

					String categoryName = asset.getFolderName();

					return new ImageAssetView(
							asset.getImageId(),
							asset.getFolderName(),
							asset.getFileName(),
							categoryName,
							asset.getUploadDate(),
							supabaseStorageService.getImageUrl(
									asset.getStoragePath()));
				})
				.toList();
	}

	/**
	 * 画像一覧をページングして取得する。
	 */
	public Page<ImageAssetView> findImagePage(
			Long userId,
			String folderName,
			Pageable pageable) {

		Page<ImageAsset> page;

		if (folderName == null) {

			page = imageAssetRepository
					.findByUserIdOrderByUploadDateDesc(
							userId,
							pageable);

		} else {

			page = imageAssetRepository
					.findByUserIdAndFolderNameOrderByUploadDateDesc(
							userId,
							folderName,
							pageable);
		}

		return page.map(a -> {

			String categoryName = a.getFolderName();

			return new ImageAssetView(
					a.getImageId(),
					a.getFolderName(),
					a.getFileName(),
					categoryName,
					a.getUploadDate(),
					supabaseStorageService.getImageUrl(
							a.getStoragePath()));
		});
	}

	/**
	 * 画像保存先フォルダ一覧を取得する。
	 *
	 * image_assetに登録されているfolderNameのみ返す。
	 */
	public List<ImageCategoryDto> findImageCategories(Long userId) {

		List<ImageCategoryDto> cachedCategories =
				imageAssetCache.getCategories(userId);

		if (cachedCategories != null) {
			return cachedCategories;
		}

		return imageAssetRepository
				.findByUserIdOrderByUploadDateDesc(userId)
				.stream()
				.map(ImageAsset::getFolderName)
				.filter(folder ->
						folder != null && !folder.isBlank())
				.distinct()
				.map(ImageCategoryDto::new)
				.toList();
	}

	/**
	 * 画像保存先フォルダ名一覧を取得する。
	 */
	public List<String> findImageFolders(Long userId) {

		List<String> cachedFolders =
				imageAssetCache.getFolders(userId);

		if (cachedFolders != null) {
			return cachedFolders;
		}

		return imageAssetRepository
				.findByUserIdOrderByUploadDateDesc(userId)
				.stream()
				.map(ImageAsset::getFolderName)
				.filter(folder ->
						folder != null && !folder.isBlank())
				.distinct()
				.toList();
	}
}