/**
 * Supabase Storage上の画像単体の操作を担当するサービス
 *
 * 一覧取得、アップロード、上書き、ダウンロード、削除、公開URL生成を担当する。
 * listAllFilePathsはフォルダを再帰的に辿り、バケット内の全ファイルパスを取得する
 * （image_asset未登録の既存画像をインポートする際に使用）。
 * 画像の移動・リネームはSupabaseStorageMoveService、
 * フォルダ単位の移動はSupabaseStorageFolderServiceが担当する。
 */
package com.app.myblogpusher.service.Supabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class SupabaseStorageService {

	private final SupabaseStorageClient storageClient;

	/**
	 * Storage操作に必要な依存関係を設定する。
	 */
	public SupabaseStorageService(SupabaseStorageClient storageClient) {
		this.storageClient = storageClient;
	}

	/**
	 * バケット直下の画像URL一覧を取得する。
	 */
	public List<String> listImages() {
		List<String> images = new ArrayList<>();

		for (JsonNode node : storageClient.listRoot()) {
			String fileName = node.get("name").asText();
			images.add(getImageUrl(fileName));
		}

		return images;
	}

	/**
	 * 変換後画像（File）をSupabase Storageの指定フォルダへアップロードする。
	 *
	 * HEIC→WebP変換後などMultipartFileではない画像を登録する場合に使用する。
	 * 戻り値はバケット内の相対パス（folderName/fileName）。
	 */
	public String uploadImage(
			File file,
			String folderName) throws IOException {

		String fileName = file.getName();
		String path = folderName + "/" + fileName;

		String contentType;

		if (fileName.toLowerCase().endsWith(".webp")) {
			contentType = "image/webp";
		} else {
			contentType = Files.probeContentType(file.toPath());
		}

		MediaType mediaType = contentType != null
				? MediaType.parseMediaType(contentType)
				: MediaType.APPLICATION_OCTET_STREAM;

		storageClient.upload(path, Files.readAllBytes(file.toPath()), mediaType);

		return path;
	}

	/**
	 * 登録済み画像を同じStorageパスへ上書きする。
	 *
	 * 記事本文から既存画像のパスを参照している可能性があるため、
	 * 差し替え時はファイルのパスを変更せず実ファイルだけ置き換える。
	 */
	public void replaceImage(
			String storagePath,
			File file) throws IOException {

		MediaType mediaType = MediaType.parseMediaType(
				Files.probeContentType(file.toPath()));

		storageClient.upsert(storagePath, Files.readAllBytes(file.toPath()), mediaType);
	}

	/**
	 * Storage上のパスから公開URLを生成する。
	 */
	public String getImageUrl(String fileName) {
		return storageClient.publicUrl(fileName);
	}

	/**
	 * バケット内の全ファイルパス（フォルダ/ファイル名）を再帰的に取得する。
	 *
	 * image_asset未登録の既存画像をインポートする際に使用する。
	 */
	public List<String> listAllFilePaths() {
		List<String> result = new ArrayList<>();
		collectFilePaths("", result);
		return result;
	}

	/**
	 * 指定プレフィックス配下のファイルパスを、フォルダを辿りながら収集する。
	 */
	private void collectFilePaths(String prefix, List<String> result) {
		for (JsonNode entry : storageClient.list(prefix)) {
			String name = entry.get("name").asText();
			boolean isFolder = entry.get("id") == null || entry.get("id").isNull();
			String path = prefix.isEmpty() ? name : prefix + "/" + name;

			if (isFolder) {
				collectFilePaths(path, result);
			} else {
				result.add(path);
			}
		}
	}

	/**
	 * Storageから画像をダウンロードする。
	 */
	public byte[] downloadImage(String storagePath) {
		return storageClient.download(storagePath);
	}

	/**
	 * Storageから画像を削除する。
	 */
	public void deleteImage(String storagePath) {
		storageClient.delete(storagePath);
	}
}