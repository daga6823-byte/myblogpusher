/**
 * 画像機能を担当するコントローラー
 *
 * DB(image_asset)に記録された画像の一覧取得、
 * Supabase Storageへのアップロード、
 * 画像削除、
 * カテゴリーに基づくデフォルトフォルダ名の取得を行う。
 */

package com.app.myblogpusher.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.app.myblogpusher.dto.ImageAssetView;
import com.app.myblogpusher.entity.ImageAsset;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Facade.ImageAssetFacadeService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ImageAssetController {

	@Autowired
	private ImageAssetFacadeService imageAssetFacadeService;

	/**
	 * DBに記録された画像一覧をJSONで返す
	 *
	 * URLだけでは削除対象を特定できないため、
	 * imageIdを含めたImageAsset情報を返す。
	 */
	@GetMapping("/article/images")
	@ResponseBody
	public List<ImageAssetView> getImages(
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		return imageAssetFacadeService.listImages(
				loginUser.getUserId(),
				null);
	}

	/**
	 * カテゴリーIDからデフォルトのフォルダ名（スラッグ）を返す
	 */
	/**
	 * 画像をアップロードし、
	 * Supabase StorageとDB(image_asset)へ登録する
	 */

	@PostMapping("/article/images/upload")
	@ResponseBody
	public Map<String, Object> upload(
			@RequestParam("files") List<MultipartFile> files,
			@RequestParam(required = false) String folderName,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<Map<String, String>> results = new java.util.ArrayList<>();

		for (MultipartFile file : files) {
			if (file == null || file.isEmpty()) {
				results.add(Map.of(
						"result", "error",
						"fileName", file == null ? "" : file.getOriginalFilename(),
						"message", "空のファイルです"));
				continue;
			}

			try {
				ImageAsset asset = imageAssetFacadeService.uploadAndRegister(
						file, folderName, userId);

				results.add(Map.of(
						"result", "ok",
						"folderName", asset.getFolderName(),
						"fileName", asset.getFileName()));

			} catch (HttpClientErrorException e) {
				results.add(Map.of(
						"result", "error",
						"fileName", file.getOriginalFilename(),
						"message", "アップロード中にエラーが発生しました"));

			} catch (IOException e) {
				results.add(Map.of(
						"result", "error",
						"fileName", file.getOriginalFilename(),
						"message", "アップロードに失敗しました"));
			}
		}

		long successCount = results.stream()
				.filter(result -> "ok".equals(result.get("result")))
				.count();

		return Map.of(
				"result", successCount == files.size() ? "ok" : "partial",
				"successCount", successCount,
				"totalCount", files.size(),
				"results", results);
	}

	/**
	 * 画像削除
	 *
	 * image_asset情報を削除し、
	 * Supabase Storage上のファイルも削除する。
	 */
	@PostMapping("/article/images/delete")
	@ResponseBody
	public Map<String, Object> delete(
			@RequestParam Long imageId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		try {

			imageAssetFacadeService.deleteImage(
					imageId,
					loginUser.getUserId());

			return Map.of(
					"result",
					"ok");

		} catch (Exception e) {

			return Map.of(
					"result",
					"error",
					"message",
					"削除に失敗しました");
		}
	}
}