/**
 * 下書き一覧機能を担当するコントローラー
 * 下書きの一覧表示と削除を管理
 */

package com.app.myblogpusher.controller.Article;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.Article.ArticleWorkView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.service.Article.ArticleWorkService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleListController {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	@GetMapping("/article/list")
	public String list(
			@RequestParam(required = false) Boolean published,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<ArticleWork> works = articleWorkService.findDrafts(userId);

		Map<Long, String> categoryPathMap = new HashMap<>();

		categoryRelationRepository.findAll()
				.forEach(relation -> {

					if (relation.getGroupId() == null
							|| relation.getCategoryPath() == null
							|| relation.getCategoryPath().isBlank()) {
						return;
					}

					categoryPathMap.putIfAbsent(
							relation.getGroupId(),
							relation.getCategoryPath());
				});

		List<ArticleWorkView> workViews = works.stream()
				.map(work -> {

					String categoryName = "（未分類）";

					if (work.getCategoryGroupId() != null) {
						categoryName = categoryPathMap.getOrDefault(
								work.getCategoryGroupId(),
								"（未分類）");
					}

					return new ArticleWorkView(
							work.getWorkId(),
							work.getTitle(),
							work.getCategoryGroupId(),
							categoryName,
							work.getUpdateDate());
				})
				.toList();

		/*
		 * カテゴリー選択用の一覧も、
		 * 下書き一覧に表示する完全なカテゴリー経路から作成する。
		 */
		List<String> categoryNames = workViews.stream()
				.map(ArticleWorkView::getCategoryName)
				.filter(name -> name != null && !name.isBlank())
				.distinct()
				.sorted()
				.toList();

		model.addAttribute("works", workViews);
		model.addAttribute("categoryNames", categoryNames);
		model.addAttribute("published", published != null && published);

		return "article/article_list";
	}

	@PostMapping("/article/delete")
	public String delete(
			@RequestParam Long workId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		articleWorkService.delete(workId, userId);

		return "redirect:/article/list";
	}
}