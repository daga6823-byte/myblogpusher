/**
 * 登録済み画像の確認画面、およびSupabase実ファイルからのインポート実行を担当するコントローラー
 */

package com.app.myblogpusher.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.ImageAssetView;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.ImageAssetRepository;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ImageAssetService;
import com.app.myblogpusher.service.SupabaseStorageService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ImageManageController {

	@Autowired
	private ImageAssetRepository imageAssetRepository;

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private SupabaseStorageService supabaseStorageService;

	@Autowired
	private ImageAssetService imageAssetService;

	private Long categoryId;
	
	/**
	 * 登録済み画像一覧表示
	 *
	 * categoryId指定時はカテゴリー内画像のみ表示。
	 * 未指定の場合は全画像表示。
	 */
	@GetMapping("/image/list")
	public String list(
			@RequestParam(required = false) Long categoryId,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		List<ImageAssetView> images = (categoryId == null
				? imageAssetRepository.findByUserIdOrderByUploadDateDesc(userId)
				: imageAssetRepository.findByUserIdAndCategoryIdOrderByUploadDateDesc(
						userId,
						categoryId))
								.stream()
								.map(a -> new ImageAssetView(
										a.getImageId(),
										a.getCategoryId(),
										a.getFolderName(),
										a.getFileName(),
										Optional.ofNullable(a.getCategoryId())
												.flatMap(articleCategoryService::findById)
												.map(ArticleCategory::getCategoryName)
												.orElse("（未分類）"),
										a.getUploadDate(),
										supabaseStorageService.getImageUrl(a.getStoragePath())))
								.toList();

		model.addAttribute(
				"images",
				images);

		model.addAttribute(
				"categories",
				articleCategoryService.findByUserId(userId));

		model.addAttribute("selectedCategoryId", categoryId);

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
				"categories",
				articleCategoryService.findByUserId(
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
}