package com.app.myblogpusher.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.TypoDictionaryView;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.TypoCorrectionService;

import jakarta.servlet.http.HttpSession;

@Controller
public class TypoDictController {

	@Autowired
	private TypoCorrectionService typoCorrectionService;

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@GetMapping("/typo-dict/list")
	public String list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(required = false) Long categoryId,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		Page<TypoDictionaryView> typoPage = typoCorrectionService.findDictionaryView(
				userId,
				page,
				keyword,
				categoryId);

		model.addAttribute(
				"typos",
				typoPage.getContent());

		model.addAttribute(
				"currentPage",
				page);

		model.addAttribute(
				"totalPages",
				typoPage.getTotalPages());

		model.addAttribute(
				"keyword",
				keyword);

		model.addAttribute(
				"categoryId",
				categoryId);

		// 新規登録・一括カテゴリー変更用
		List<ArticleCategory> categories = articleCategoryService.findByUserId(userId);

		model.addAttribute(
				"categories",
				categories);

		return "typo_dict";
	}

	@PostMapping("/typo-dict/update")
	@ResponseBody
	public Map<String, String> update(@RequestParam Long typoId,
			@RequestParam String wrongWord,
			@RequestParam String correctWord,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		typoCorrectionService.update(typoId, wrongWord, correctWord, userId);

		return Map.of("result", "ok");
	}

	@PostMapping("/typo-dict/delete")
	public String delete(@RequestParam Long typoId, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		typoCorrectionService.delete(typoId, userId);

		return "redirect:/typo-dict/list";
	}

	// 新規登録（この画面から直接）
	@PostMapping("/typo-dict/add")
	public String add(@RequestParam String wrongWord,
			@RequestParam String correctWord,
			@RequestParam(required = false) Long categoryId,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		typoCorrectionService.insertTypo(categoryId, wrongWord, correctWord, userId);

		return "redirect:/typo-dict/list";
	}

	// 一括カテゴリー変更
	@PostMapping("/typo-dict/bulk-category")
	public String bulkUpdateCategory(@RequestParam List<Long> typoIds,
			@RequestParam(required = false) Long categoryId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		typoCorrectionService.updateCategory(typoIds, categoryId, userId);

		return "redirect:/typo-dict/list";
	}

	// 一括削除
	@PostMapping("/typo-dict/bulk-delete")
	public String bulkDelete(@RequestParam List<Long> typoIds, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		typoCorrectionService.deleteAll(typoIds, userId);

		return "redirect:/typo-dict/list";
	}
}