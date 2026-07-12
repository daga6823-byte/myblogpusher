/**
 * 画像機能を担当するコントローラー
 * DB(image_asset)に記録された画像の一覧取得、Supabase Storageへのアップロード、
 * カテゴリーに基づくデフォルトフォルダ名の取得を行う
 */

package com.app.myblogpusher.controller.Article;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.app.myblogpusher.entity.ImageAsset;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ImageAssetService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleImageController {

	@Autowired
	private ImageAssetService imageAssetService;

	/**
	 * DBに記録された画像一覧をJSONで返す（categoryId指定で絞り込み可）
	 */
	@GetMapping("/article/images")
	@ResponseBody
	public List<String> getImages(@RequestParam(required = false) Long categoryId, HttpSession session) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		return imageAssetService.listImageUrls(loginUser.getUserId(), categoryId);
	}

	/**
	 * カテゴリーIDからデフォルトのフォルダ名（スラッグ）を返す
	 */
	@GetMapping("/article/images/default-folder")
	@ResponseBody
	public Map<String, String> getDefaultFolder(@RequestParam Long categoryId) {
		return Map.of("folderName", imageAssetService.resolveDefaultFolderName(categoryId));
	}

	/**
	 * 画像をアップロードし、Supabase StorageとDB(image_asset)の両方に記録する
	 * folderNameが空ならcategoryIdから決まるデフォルトフォルダを使う
	 */
	@PostMapping("/article/images/upload")
	@ResponseBody
	public Map<String, Object> upload(
			@RequestParam MultipartFile file,
			@RequestParam Long categoryId,
			@RequestParam(required = false) String folderName,
			@RequestParam(required = false) Long workId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		try {
			ImageAsset asset = imageAssetService.uploadAndRegister(file, folderName, categoryId, workId, userId);
			return Map.of(
					"result", "ok",
					"folderName", asset.getFolderName(),
					"fileName", asset.getFileName());
		} catch (IOException e) {
			return Map.of("result", "error", "message", "アップロードに失敗しました");
		}
	}
}