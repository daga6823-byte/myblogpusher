/**
 * Supabase Storage上の画像の移動・リネームを担当するサービス
 *
 * Storageには移動APIを使わず、
 * ダウンロード → 新パスへアップロード → 元ファイル削除の順で移動する。
 * 新しいパスへの登録に成功してから元ファイルを削除する。
 */
package com.app.myblogpusher.service.Supabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class SupabaseStorageMoveService {

	private final SupabaseStorageClient storageClient;

	/**
	 * 移動・リネームに必要な依存関係を設定する。
	 */
	public SupabaseStorageMoveService(SupabaseStorageClient storageClient) {
		this.storageClient = storageClient;
	}

	/**
	 * 画像を別フォルダへ移動する。ファイル名は変更しない。
	 *
	 * @return 新しいstoragePath
	 */
	public String moveImage(
			String oldPath,
			String newFolderName) {

		String fileName = oldPath.substring(
				oldPath.lastIndexOf('/') + 1);

		String newPath = newFolderName + "/" + fileName;

		moveStoragePath(oldPath, newPath);

		return newPath;
	}

	/**
	 * Storage上の画像を別のフォルダ・ファイル名へ変更する。
	 *
	 * uploadFileが指定されている場合は、そのファイルを新しいパスへ
	 * アップロードしてから元画像を削除する。
	 *
	 * uploadFileがnullの場合は、現在の画像をStorageから取得して
	 * 新しいパスへ移動する。
	 *
	 * @return 新しいstoragePath
	 */
	public String renameImage(
			String oldPath,
			String newFolderName,
			String newFileName,
			File uploadFile) throws IOException {

		String newPath = newFolderName + "/" + newFileName;

		byte[] image;

		if (uploadFile != null) {
			image = Files.readAllBytes(uploadFile.toPath());
		} else {
			image = storageClient.download(oldPath);
		}

		storageClient.upload(newPath, image, MediaType.APPLICATION_OCTET_STREAM);

		// 新しいファイルの登録に成功してから元ファイルを削除する。
		if (!oldPath.equals(newPath)) {
			storageClient.delete(oldPath);
		}

		return newPath;
	}

	/**
	 * Storage上のファイルを指定パスへ移動する。
	 */
	public void moveStoragePath(
			String oldPath,
			String newPath) {

		byte[] image = storageClient.download(oldPath);

		storageClient.upload(newPath, image, MediaType.APPLICATION_OCTET_STREAM);

		// 新しいパスへのアップロード成功後に元ファイルを削除する。
		storageClient.delete(oldPath);
	}
}