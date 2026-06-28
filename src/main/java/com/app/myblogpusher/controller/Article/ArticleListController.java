package com.app.myblogpusher.controller.Article;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.ArticleWorkView;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleWorkService;

import jakarta.servlet.http.HttpSession;

/**
 * 下書き一覧機能を担当するコントローラー
 * 下書きの一覧表示と削除を管理
 */
@Controller
public class ArticleListController {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleCategoryService articleCategoryService;

	/**
	 * 下書き一覧画面を表示
	 */
	@GetMapping("/article/list")
	public String list(
			@RequestParam(required = false) Boolean published,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<ArticleWork> works = articleWorkService.findByUserId(userId);

		List<ArticleWorkView> workViews = works.stream()
				.map(work -> {
					String categoryName = articleCategoryService.findById(work.getCategoryId())
							.map(ArticleCategory::getCategoryName)
							.orElse("（未分類）");
					return new ArticleWorkView(work.getWorkId(), work.getTitle(), categoryName, work.getUpdateDate());
				})
				.toList();

		model.addAttribute("works", workViews);
		model.addAttribute("published", published != null && published);

		return "article_list";
	}

	/**
	 * 下書きを削除
	 */
	@PostMapping("/article/delete")
	public String delete(@RequestParam Long workId, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		articleWorkService.delete(workId, userId);

		return "redirect:/article/list";
	}
}