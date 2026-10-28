/**
 * 画像形式変換を担当するサービス
 *
 * HEIC画像をWebPへ変換する。
 * その他の形式はそのまま返却する。
 */

package com.app.myblogpusher.service.Image;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageConvertService {

	/**
	 * 画像を検証し、必要に応じてWebPへ変換する。
	 *
	 * JPEG・PNG・GIF・WebP・HEICを許可する。
	 * 画像形式、画像サイズ、画素数を検証し、
	 * 外部プロセスには時間・メモリ・ディスクの制限を設定する。
	 */
	public File convert(MultipartFile file) throws IOException {

		String originalName = file.getOriginalFilename();

		if (originalName == null || originalName.lastIndexOf('.') < 0) {
			throw new IOException("ファイル名が取得できません");
		}

		String extension = getExtension(originalName);

		if (!List.of("jpg", "jpeg", "png", "gif", "webp", "heic")
				.contains(extension)) {
			throw new IOException("許可されていない画像形式です");
		}

		// 拡張子とファイルシグネチャを照合する。
		if (!isAllowedImage(file)) {
			throw new IOException("拡張子と画像データが一致しません");
		}

		File input = null;
		File output = null;
		File identifyOutput = null;
		File identifyError = null;
		boolean converted = false;

		try {
			input = Files.createTempFile("upload_", "." + extension).toFile();
			file.transferTo(input);

			identifyOutput = Files.createTempFile("identify_", ".txt").toFile();
			identifyError = Files.createTempFile("identify_error_", ".txt").toFile();

			ProcessBuilder identifyBuilder = new ProcessBuilder(
			        "magick",
			        "-limit", "time", "10",
			        "-limit", "memory", "128MiB",
			        "-limit", "map", "256MiB",
			        "-limit", "disk", "512MiB",
			        "identify",
			        "-format", "%m %w %h",
			        input.getAbsolutePath());

			identifyBuilder.redirectError(identifyError);
			identifyBuilder.redirectOutput(identifyOutput);

			Process identifyProcess = identifyBuilder.start();

			if (!identifyProcess.waitFor(12, TimeUnit.SECONDS)) {
				identifyProcess.destroyForcibly();
				identifyProcess.waitFor();
				throw new IOException("画像検証がタイムアウトしました");
			}

			if (identifyProcess.exitValue() != 0) {
			    String errorMessage = Files.readString(
			            identifyError.toPath(),
			            StandardCharsets.UTF_8).trim();

			    throw new IOException(
			            "正常な画像として読み込めません。ImageMagickのエラー: "
			                    + errorMessage);
			}

			String[] imageInfo = Files.readString(
					identifyOutput.toPath(),
					StandardCharsets.UTF_8).trim().split("\\s+");

			if (imageInfo.length != 3) {
				throw new IOException("画像情報を取得できません");
			}

			String format = imageInfo[0].toUpperCase(Locale.ROOT);
			long width;
			long height;

			try {
				width = Long.parseLong(imageInfo[1]);
				height = Long.parseLong(imageInfo[2]);
			} catch (NumberFormatException e) {
				throw new IOException("画像サイズが不正です", e);
			}

			String expectedFormat;

			switch (extension) {
			case "jpg":
			case "jpeg":
				expectedFormat = "JPEG";
				break;
			case "png":
				expectedFormat = "PNG";
				break;
			case "gif":
				expectedFormat = "GIF";
				break;
			case "webp":
				expectedFormat = "WEBP";
				break;
			case "heic":
				expectedFormat = "HEIC";
				break;
			default:
				throw new IOException("許可されていない画像形式です");
			}

			if (!expectedFormat.equals(format)) {
				throw new IOException("拡張子と画像形式が一致しません");
			}

			if (width <= 0 || height <= 0
					|| width > 10000 || height > 10000
					|| width * height > 40_000_000L) {
				throw new IOException("画像サイズが上限を超えています");
			}

			// HEIC以外は検証済みの元ファイルを返す。
			if (!extension.equals("heic")) {
				File validatedFile = input;
				input = null;
				return validatedFile;
			}

			output = Files.createTempFile("convert_", ".webp").toFile();

			ProcessBuilder convertBuilder = new ProcessBuilder(
					"magick",
					"-limit", "time", "10",
					"-limit", "memory", "128MiB",
					"-limit", "map", "256MiB",
					"-limit", "disk", "512MiB",
					input.getAbsolutePath(),
					output.getAbsolutePath());

			convertBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

			Process convertProcess = convertBuilder.start();

			if (!convertProcess.waitFor(12, TimeUnit.SECONDS)) {
				convertProcess.destroyForcibly();
				convertProcess.waitFor();
				throw new IOException("画像変換がタイムアウトしました");
			}

			if (convertProcess.exitValue() != 0
					|| !output.isFile()
					|| output.length() == 0) {
				throw new IOException("画像変換に失敗しました");
			}

			File convertedFile = output;
			output = null;
			converted = true;
			return convertedFile;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("画像処理が中断されました", e);

		} finally {
			if (input != null) {
				input.delete();
			}
			if (output != null) {
				output.delete();
			}
			if (identifyOutput != null) {
			    identifyOutput.delete();
			}
			if (identifyError != null) {
			    identifyError.delete();
			}
		}
	}

	private String getExtension(String name) {

		int index = name.lastIndexOf('.');

		if (index == -1) {
			return "";
		}

		return name.substring(index + 1)
				.toLowerCase();
	}

	/**
	 * 画像ファイルの拡張子と先頭シグネチャを検証する。
	 *
	 * JPEG・PNG・GIF・WebP・HEICを許可する。
	 */
	private boolean isAllowedImage(MultipartFile file) throws IOException {
		String fileName = file.getOriginalFilename();

		if (fileName == null || fileName.lastIndexOf('.') < 0) {
			return false;
		}

		String extension = getExtension(fileName);

		try (var inputStream = file.getInputStream()) {
			byte[] header = inputStream.readNBytes(12);

			switch (extension) {
			case "jpg":
			case "jpeg":
				return header.length >= 3
						&& (header[0] & 0xFF) == 0xFF
						&& (header[1] & 0xFF) == 0xD8
						&& (header[2] & 0xFF) == 0xFF;

			case "png":
				return header.length >= 8
						&& (header[0] & 0xFF) == 0x89
						&& header[1] == 0x50
						&& header[2] == 0x4E
						&& header[3] == 0x47
						&& header[4] == 0x0D
						&& header[5] == 0x0A
						&& header[6] == 0x1A
						&& header[7] == 0x0A;

			case "gif":
				return header.length >= 6
						&& new String(header, 0, 6, StandardCharsets.US_ASCII)
								.matches("GIF87a|GIF89a");

			case "webp":
				return header.length >= 12
						&& new String(header, 0, 4, StandardCharsets.US_ASCII)
								.equals("RIFF")
						&& new String(header, 8, 4, StandardCharsets.US_ASCII)
								.equals("WEBP");

			case "heic":
				return header.length >= 12
						&& new String(header, 4, 8, StandardCharsets.US_ASCII)
								.contains("ftyp");

			default:
				return false;
			}
		}
	}

}