/**
 * 画像形式変換を担当するサービス
 *
 * HEIC画像をWebPへ変換する。
 * その他の形式はそのまま返却する。
 */

package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageConvertService {

	/**
	 * 画像を必要に応じてWebPへ変換する
	 *
	 * HEIC → WebP
	 * その他 → 元ファイル
	 */
	public File convert(MultipartFile file) throws IOException {

		String originalName = file.getOriginalFilename();

		if (originalName == null) {
			throw new IOException("ファイル名が取得できません");
		}

		String extension = getExtension(originalName);

		// HEIC以外は変換不要
		if (!extension.equals("heic")) {

			File temp = Files.createTempFile(
					"upload_",
					"." + extension)
					.toFile();

			file.transferTo(temp);

			return temp;
		}

		// 一時HEICファイル
		File input = Files.createTempFile(
				"upload_",
				".heic")
				.toFile();

		file.transferTo(input);

		// 出力WebP
		File output = Files.createTempFile(
				"convert_",
				".webp")
				.toFile();

		ProcessBuilder builder = new ProcessBuilder(
				"magick",
				input.getAbsolutePath(),
				output.getAbsolutePath());

		Process process = builder.start();

		try {

			int result = process.waitFor();

			if (result != 0) {
				throw new IOException(
						"画像変換に失敗しました");
			}

		} catch (InterruptedException e) {

			Thread.currentThread().interrupt();
			throw new IOException(
					"画像変換処理が中断されました",
					e);
		}

		input.delete();

		return output;
	}

	private String getExtension(String name) {

		int index = name.lastIndexOf('.');

		if (index == -1) {
			return "";
		}

		return name.substring(index + 1)
				.toLowerCase();
	}

}