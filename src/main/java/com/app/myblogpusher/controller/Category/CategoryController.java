/**
 * カテゴリー辞典を管理するコントローラー
 *
 * カテゴリー一覧表示、
 * カテゴリーの追加・更新・削除を担当する。
 */

package com.app.myblogpusher.controller.Category;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.Category.CategoryDictionaryView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Article.ArticleCategoryService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CategoryController {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	/**
	 * カテゴリー辞典一覧を表示
	 */
	@GetMapping("/category/list")
	public String list(HttpSession session, Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<CategoryDictionaryView> categories = articleCategoryService.findDictionaryView(userId);
		model.addAttribute("categories", categories);

		return "category/category_list";
	}

	/**
	 * カテゴリーを新規登録する
	 *
	 * 同名カテゴリーが存在する場合は登録しない。
	 */
	@PostMapping("/category/add")
	@ResponseBody
	public Map<String, String> add(@RequestParam String categoryName,
			@RequestParam(required = false) Long parentCategoryId,
			@RequestParam String displayName, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		try {

			articleCategoryService.insertCategory(
					userId,
					categoryName,
					parentCategoryId,
					displayName);

			return Map.of("result", "ok");

		} catch (IllegalArgumentException e) {

			return Map.of(
					"result", "error",
					"message", e.getMessage());
		}

	}

	/**
	 * カテゴリー情報を更新する
	 *
	 * カテゴリー名、表示名、親カテゴリーを更新する。
	 * 同名カテゴリーが存在する場合は更新しない。
	 */
	@PostMapping("/category/update")
	@ResponseBody
	public Map<String, String> updateCategory(
			@RequestParam Long categoryId,
			@RequestParam String newName,
			@RequestParam String displayName,
			@RequestParam(required = false) Long parentCategoryId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		try {

			articleCategoryService.update(
					categoryId,
					userId,
					newName,
					parentCategoryId,
					displayName);

			return Map.of("result", "ok");

		} catch (IllegalArgumentException e) {

			return Map.of(
					"result",
					"error",
					"message",
					e.getMessage());
		}
	}

	/**
	 * カテゴリーを削除する
	 *
	 * 使用中カテゴリーはサービス側で削除可否を判定する。
	 */
	@PostMapping("/category/delete")
	public String delete(@RequestParam Long categoryId, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		articleCategoryService.delete(categoryId, userId);

		return "redirect:/category/list";
	}
}