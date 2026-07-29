/**
 * 登録済み画像の確認画面、およびSupabase実ファイルからのインポート実行を担当するコントローラー
 */

package com.app.myblogpusher.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.ImageAssetView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Image.ImageAssetService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ImageManageController {

	@Autowired
	private ImageAssetService imageAssetService;

	/**
	 * 登録済み画像一覧表示
	 *
	 * categoryId指定時はカテゴリー内画像のみ表示。
	 * 未指定の場合は全画像表示。
	 */
	@GetMapping("/image/list")
	public String list(
			@RequestParam(required = false) String folderName,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		Pageable pageable = PageRequest.of(page, size);

		Page<ImageAssetView> imagePage = imageAssetService.findImagePage(userId, folderName, pageable);

		model.addAttribute("images", imagePage.getContent());

		model.addAttribute(
				"imageCategories",
				imageAssetService.findImageCategories(userId));

		model.addAttribute("selectedFolderName", folderName);

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", imagePage.getTotalPages());
		model.addAttribute("pageSize", size);

		return "image_list";
	}

	@PostMapping("/image/import")
	@ResponseBody
	public Map<String, Object> importImages(HttpSession session) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		int count = imageAssetService.importExistingImages(loginUser.getUserId());
		return Map.of("result", "ok", "importedCount", count);
	}

	/**
	 * 画像新規登録画面表示
	 */
	@GetMapping("/image/new")
	public String newImage(
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		model.addAttribute(
				"imageCategories",
				imageAssetService.findImageCategories(
						loginUser.getUserId()));

		return "image_new";
	}

	@PostMapping("/image/update")
	@ResponseBody
	public Map<String, Object> updateImage(
			@RequestParam Long imageId,
			@RequestParam(required = false) Long categoryId,
			@RequestParam String folderName,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		imageAssetService.updateImage(
				imageId,
				categoryId,
				folderName,
				loginUser.getUserId());

		return Map.of("result", "ok");
	}

	/**
	 * 画像フォルダー一覧を取得する
	 *
	 * 画像選択モーダルのカテゴリー一覧再読込で使用する。
	 */
	@GetMapping("/image/categories")
	@ResponseBody
	public Object imageCategories(HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		return imageAssetService.findImageCategories(
				loginUser.getUserId());
	}

	/**
	 * 保存先フォルダ一覧取得
	 *
	 * image_assetに登録済みのfolderNameのみ返す。
	 */
	@GetMapping("/image/folders")
	@ResponseBody
	public List<String> folders(HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		return imageAssetService.findImageFolders(
				loginUser.getUserId());

	}
}