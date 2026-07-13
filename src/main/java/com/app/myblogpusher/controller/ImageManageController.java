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

	@GetMapping("/image/list")
	public String list(HttpSession session, Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<ImageAssetView> images = imageAssetRepository.findByUserIdOrderByUploadDateDesc(userId).stream()
				.map(a -> new ImageAssetView(
						a.getImageId(),
						a.getFolderName(),
						a.getFileName(),
						Optional.ofNullable(a.getCategoryId())
								.flatMap(articleCategoryService::findById)
								.map(ArticleCategory::getCategoryName)
								.orElse("（未分類）"),
						a.getUploadDate(),
						supabaseStorageService.getImageUrl(a.getStoragePath())))
				.toList();

		model.addAttribute("images", images);
		return "image_list";
	}

	@PostMapping("/image/import")
	@ResponseBody
	public Map<String, Object> importImages(HttpSession session) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		int count = imageAssetService.importExistingImages(loginUser.getUserId());
		return Map.of("result", "ok", "importedCount", count);
	}
}