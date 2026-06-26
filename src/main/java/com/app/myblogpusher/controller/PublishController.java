package com.app.myblogpusher.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.PublishPreviewForm;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.ArticleWorkRepository;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.GitHubPushService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PublishController {

	private final ArticleWorkService articleWorkService;
	private final UserRepositoryRepository userRepositoryRepository;
	private final ArticleCategoryService articleCategoryService;
	private final GitHubPushService gitHubPushService;

	public PublishController(ArticleWorkRepository articleWorkRepository,
			ArticleWorkService articleWorkService,
			UserRepositoryRepository userRepositoryRepository,
			ArticleCategoryService articleCategoryService,
			GitHubPushService gitHubPushService) {
		this.articleWorkService = articleWorkService;
		this.userRepositoryRepository = userRepositoryRepository;
		this.articleCategoryService = articleCategoryService;
		this.gitHubPushService = gitHubPushService;
	}

	@PostMapping("/publish/preview")
	public String showPreview(@RequestParam(required = false) Long workId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			HttpSession session,
			Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// カテゴリーIDを取得
		String categoryName = "__new__".equals(categorySelect) ? newCategoryName : categorySelect;
		Long categoryId = articleCategoryService.findByUserIdAndName(userId, categoryName)
				.map(ArticleCategory::getCategoryId)
				.orElseGet(() -> articleCategoryService.insertCategory(userId, categoryName));

		// リポジトリ情報取得
		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);
		if (repoOpt.isEmpty()) {
			model.addAttribute("error", "リポジトリが設定されていません");
			return "error";
		}

		UserRepositoryEntity repo = repoOpt.get();

		PublishPreviewForm form = new PublishPreviewForm();
		form.setArticleId(workId);
		form.setArticleTitle(title);
		form.setArticleContent(content);
		form.setSlug(generateSlug(title));
		form.setCategoryId(categoryId);
		form.setRepoOwner(repo.getRepoOwner());
		form.setRepoName(repo.getRepoName());

		model.addAttribute("form", form);
		return "publish_preview";
	}

	private String generateSlug(String title) {

		return title
				.toLowerCase()
				.replaceAll("[^a-zA-Z0-9ぁ-んァ-ン一-龥ー]", "-")
				.replaceAll("-+", "-")
				.replaceAll("^-|-$", "");
	}

	@PostMapping("/publish/execute")
	public String executePublish(@RequestParam(required = false) Long workId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam String slug,
			@RequestParam Long categoryId,
			HttpSession session,
			Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// リポジトリ情報取得
		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);
		if (repoOpt.isEmpty()) {
			model.addAttribute("error", "リポジトリが設定されていません");
			return "error";
		}

		UserRepositoryEntity repo = repoOpt.get();

		try {
			// GitHub プッシュ実行
			gitHubPushService.pushArticle(
					repo,
					loginUser.getCipherKey(),
					categoryId,
					title,
					content,
					slug);

			// article_workに保存
			if (workId != null) {
				// 既存レコードを更新
				articleWorkService.updateArticleWork(
						workId,
						categoryId,
						title,
						content,
						userId,
						slug);
			} else {
				// 新規作成
				articleWorkService.insertArticleWork(userId, categoryId, title, content, slug);
			}

			return "redirect:/article/list?published";
		} catch (Exception e) {
			model.addAttribute("error", "投稿に失敗しました: " + e.getMessage());
			return "error";
		}
	}
}