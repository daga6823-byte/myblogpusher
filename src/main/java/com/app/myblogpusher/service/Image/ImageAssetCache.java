/**
 * 画像情報の一時キャッシュを管理するクラス
 *
 * ユーザーごとに画像一覧の初回表示に必要な情報を
 * Javaのメモリ上へ一時的に保持する。
 *
 * キャッシュが存在しない場合は、通常どおりDBから取得する。
 * サーバー再起動時にはキャッシュは破棄される。
 */
package com.app.myblogpusher.service.Image;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.app.myblogpusher.dto.ImageAssetView;
import com.app.myblogpusher.dto.ImageCategoryDto;

@Component
public class ImageAssetCache {

	/**
	 * モーダル用の画像一覧キャッシュ
	 */
	private final ConcurrentMap<Long, List<ImageAssetView>> imageCache = new ConcurrentHashMap<>();

	/**
	 * 画像一覧画面用の全画像キャッシュ
	 */
	private final ConcurrentMap<Long, List<ImageAssetView>> allImagesCache = new ConcurrentHashMap<>();

	/**
	 * ユーザー・フォルダーごとの画像一覧キャッシュ
	 */
	private final ConcurrentMap<String, List<ImageAssetView>> folderImageCache = new ConcurrentHashMap<>();

	private final ConcurrentMap<Long, List<ImageCategoryDto>> categoryCache = new ConcurrentHashMap<>();

	private final ConcurrentMap<Long, List<String>> folderCache = new ConcurrentHashMap<>();

	public void putImages(Long userId, List<ImageAssetView> images) {
		imageCache.put(userId, images);
	}

	/**
	 * 一覧画面用の全画像をキャッシュする。
	 */
	public void putAllImages(Long userId, List<ImageAssetView> images) {
		allImagesCache.put(userId, images);
	}

	/**
	 * 一覧画面用の全画像をキャッシュから取得する。
	 */
	public List<ImageAssetView> getAllImages(Long userId) {
		return allImagesCache.get(userId);
	}

	/**
	 * モーダル用の画像一覧をキャッシュから取得する。
	 */
	public List<ImageAssetView> getImages(Long userId) {
		return imageCache.get(userId);
	}

	/**
	 * フォルダー別の画像一覧をキャッシュする。
	 */
	public void putFolderImages(
			Long userId,
			String folderName,
			List<ImageAssetView> images) {

		folderImageCache.put(userId + ":" + folderName, images);
	}

	/**
	 * フォルダー別の画像一覧をキャッシュから取得する。
	 * キャッシュが存在しない場合はnullを返す。
	 */
	public List<ImageAssetView> getFolderImages(
			Long userId,
			String folderName) {

		return folderImageCache.get(userId + ":" + folderName);
	}

	public void putCategories(
			Long userId,
			List<ImageCategoryDto> categories) {

		categoryCache.put(userId, categories);
	}

	public List<ImageCategoryDto> getCategories(Long userId) {
		return categoryCache.get(userId);
	}

	public void putFolders(Long userId, List<String> folders) {
		folderCache.put(userId, folders);
	}

	public List<String> getFolders(Long userId) {
		return folderCache.get(userId);
	}

	/**
	 * ユーザーの画像キャッシュをすべて削除する。
	 *
	 * アップロード・削除などで画像情報が変更された場合に使用する。
	 */
	public void clear(Long userId) {
		imageCache.remove(userId);
		folderImageCache.keySet().removeIf(
				key -> key.startsWith(userId + ":"));
		categoryCache.remove(userId);
		folderCache.remove(userId);
	}
}