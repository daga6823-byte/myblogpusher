package com.app.myblogpusher.controller.Article;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleFormatService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.ArticleWorkspaceService;
import com.app.myblogpusher.util.SlugUtil;

import jakarta.servlet.http.HttpSession;

/**
 * 記事編集機能を担当するコントローラー
 * 編集画面表示、下書き保存、添削処理を管理
 */
@Controller
public class ArticleEditController {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleWorkspaceService workspaceService;

	@Autowired
	private ArticleFormatService articleFormatService;

	@Autowired
	private SlugUtil slugUtil;
	
	/**
	 * 記事編集画面を表示
	 * 下書きがあれば表示、無ければworkspaceから復元
	 */
	@GetMapping("/article/edit")
	public String editForm(@RequestParam(required = false) Long workId,
			@RequestParam(required = false) Boolean saved,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<ArticleCategory> categories = articleCategoryService.findByUserId(userId);
		model.addAttribute("categories", categories);

		if (workId != null) {
			ArticleWork work = articleWorkService.findById(workId);
			model.addAttribute("work", work);

			String categoryName = articleCategoryService.findById(work.getCategoryId())
					.map(ArticleCategory::getCategoryName)
					.orElse("");
			model.addAttribute("categoryName", categoryName);
		} else {
			workspaceService.find(userId)
					.ifPresent(ws -> {
						ArticleWork work = new ArticleWork();
						work.setTitle(ws.getTitle());
						work.setContent(ws.getContent());
						work.setCategoryId(ws.getCategoryId());
						model.addAttribute("work", work);

						if (ws.getCategoryId() != null) {
							articleCategoryService.findById(ws.getCategoryId())
									.ifPresent(category -> model.addAttribute("categoryName", category.getCategoryName()));
						}
					});
		}

		model.addAttribute("saved", saved != null && saved);
		return "article/article_edit";
	}

	/**
	 * 下書きを保存
	 */
	@PostMapping("/article/save")
	public String saveDraft(@RequestParam(required = false) Long workId,
			@RequestParam String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam(required = false) String redirectTo,
			HttpSession session) {

		Long savedWorkId = doSaveDraft(workId, categorySelect, newCategoryName, title, content, session);

		if (savedWorkId == null) {
			return "redirect:/article/edit";
		}

		if ("home".equals(redirectTo)) {
			return "redirect:/home";
		}
		if ("list".equals(redirectTo)) {
			return "redirect:/article/list";
		}

		return "redirect:/article/edit?workId=" + savedWorkId + "&saved=true";
	}

	/**
	 * 下書きを整形して保存
	 */
	private Long doSaveDraft(Long workId, String categorySelect, String newCategoryName,
			String title, String content, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		String categoryName = "__new__".equals(categorySelect) ? newCategoryName : categorySelect;
		Long categoryId = articleCategoryService.findByUserIdAndName(userId, categoryName)
				.map(ArticleCategory::getCategoryId)
				.orElseGet(() -> articleCategoryService.insertCategory(userId, categoryName));

		String formattedContent = articleFormatService.formatContent(content);
	
		String slug = slugUtil.generateSlug(title);

		if (workId == null) {
			if ((title == null || title.isBlank()) && (formattedContent == null || formattedContent.isBlank())) {
				return null;
			}

			Optional<ArticleWork> existing = articleWorkService.findDuplicate(userId, categoryId, title, formattedContent);
			if (existing.isPresent()) {
				return existing.get().getWorkId();
			}

			return articleWorkService.insertArticleWork(userId, categoryId, title, content, slug);
		} else {
			articleWorkService.updateArticleWork(workId, categoryId, title, content, userId, slug);
			return workId;
		}
	}
}