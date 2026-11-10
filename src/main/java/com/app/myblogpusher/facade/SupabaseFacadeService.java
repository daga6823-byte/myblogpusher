/**
 * Supabase Storage操作の窓口となるファサードクラス
 *
 * 画像単体の操作はSupabaseStorageService、移動・リネームはSupabaseStorageMoveService、
 * フォルダ単位の移動とロールバックはSupabaseStorageFolderServiceへ委譲する。
 * 呼び出し元は、このクラスだけを注入すればStorage操作をすべて行える。
 */
package com.app.myblogpusher.facade;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.service.Supabase.SupabaseStorageFolderService;
import com.app.myblogpusher.service.Supabase.SupabaseStorageMoveService;
import com.app.myblogpusher.service.Supabase.SupabaseStorageService;

@Service
public class SupabaseFacadeService {

	private final SupabaseStorageService storageService;
	private final SupabaseStorageMoveService moveService;
	private final SupabaseStorageFolderService folderService;

	/**
	 * Storage操作の委譲先となる各サービスを設定する。
	 */
	public SupabaseFacadeService(
			SupabaseStorageService storageService,
			SupabaseStorageMoveService moveService,
			SupabaseStorageFolderService folderService) {
		this.storageService = storageService;
		this.moveService = moveService;
		this.folderService = folderService;
	}

	// ===== 画像単体の操作(SupabaseStorageService) =====

	/**
	 * バケット直下の画像URL一覧を取得する。
	 */
	public List<String> listImages() {
		return storageService.listImages();
	}

	/**
	 * 変換後画像（File）をSupabase Storageの指定フォルダへアップロードする。
	 *
	 * @return バケット内の相対パス（folderName/fileName）
	 */
	public String uploadImage(
			File file,
			String folderName) throws IOException {
		return storageService.uploadImage(file, folderName);
	}

	/**
	 * 登録済み画像を同じStorageパスへ上書きする。
	 */
	public void replaceImage(
			String storagePath,
			File file) throws IOException {
		storageService.replaceImage(storagePath, file);
	}

	/**
	 * Storage上のパスから公開URLを生成する。
	 */
	public String getImageUrl(String fileName) {
		return storageService.getImageUrl(fileName);
	}

	/**
	 * バケット内の全ファイルパス（フォルダ/ファイル名）を再帰的に取得する。
	 */
	public List<String> listAllFilePaths() {
		return storageService.listAllFilePaths();
	}

	/**
	 * Storageから画像をダウンロードする。
	 */
	public byte[] downloadImage(String storagePath) {
		return storageService.downloadImage(storagePath);
	}

	/**
	 * Storageから画像を削除する。
	 */
	public void deleteImage(String storagePath) {
		storageService.deleteImage(storagePath);
	}

	// ===== 画像の移動・リネーム(SupabaseStorageMoveService) =====

	/**
	 * 画像を別フォルダへ移動する。ファイル名は変更しない。
	 *
	 * @return 新しいstoragePath
	 */
	public String moveImage(
			String oldPath,
			String newFolderName) {
		return moveService.moveImage(oldPath, newFolderName);
	}

	/**
	 * Storage上の画像を別のフォルダ・ファイル名へ変更する。
	 *
	 * @return 新しいstoragePath
	 */
	public String renameImage(
			String oldPath,
			String newFolderName,
			String newFileName,
			File uploadFile) throws IOException {
		return moveService.renameImage(oldPath, newFolderName, newFileName, uploadFile);
	}

	// ===== フォルダ単位の操作(SupabaseStorageFolderService) =====

	/**
	 * フォルダ配下の画像を別フォルダへ移動する。
	 *
	 * @return 移動前・移動後のStorageパスの対応表
	 */
	public Map<String, String> renameFolder(
			String oldFolderName,
			String newFolderName) {
		return folderService.renameFolder(oldFolderName, newFolderName);
	}

	/**
	 * フォルダ移動後にDB更新が失敗した場合、移動済みのファイルだけを元のパスへ戻す。
	 */
	public void restoreMovedPaths(Map<String, String> movedPaths) {
		folderService.restoreMovedPaths(movedPaths);
	}
}