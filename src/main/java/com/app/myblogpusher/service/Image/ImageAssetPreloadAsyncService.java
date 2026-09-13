/**
 * ログイン時の画像情報先読みを担当する非同期Service
 *
 * ログイン直後に画像一覧の初回表示に必要な情報を
 * バックグラウンドで取得し、キャッシュへ保持する。
 *
 * 画像ファイル自体は取得せず、画像一覧の情報だけを先読みする。
 */
package com.app.myblogpusher.service.Image;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.ImageAssetView;
import com.app.myblogpusher.dto.ImageCategoryDto;

@Service
public class ImageAssetPreloadAsyncService {

	@Autowired
	private ImageAssetService imageAssetService;

	@Autowired
	private ImageAssetCache imageAssetCache;

	/**
	 * ログイン時に画像一覧の初回表示に必要な情報を非同期で取得する。
	 */
	public void preloadAsync(Long userId) {
		CompletableFuture.runAsync(() -> {
			try {
				// 初回表示用として1ページ目の12件だけ取得する。
				Page<ImageAssetView> imagePage = imageAssetService.findImagePage(
						userId,
						null,
						PageRequest.of(0, 12));

				// 画像カテゴリー一覧を取得する。
				List<ImageCategoryDto> categories = imageAssetService.findImageCategories(userId);

				// 保存先フォルダ一覧を取得する。
				List<String> folders = imageAssetService.findImageFolders(userId);

				// 取得した情報をユーザー単位でメモリキャッシュへ保存する。
				imageAssetCache.putImages(userId, imagePage);
				imageAssetCache.putCategories(userId, categories);
				imageAssetCache.putFolders(userId, folders);

			} catch (Exception e) {
				// 先読み失敗でログイン処理へ影響を与えない。
				System.err.println(
						"画像一覧の先読み中にエラーが発生しました: "
								+ e.getMessage());
			}
		});
	}
}