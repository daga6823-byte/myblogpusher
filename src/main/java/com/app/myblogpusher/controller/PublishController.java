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
import com.app.myblogpusher.util.SlugUtil;

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
		form.setSlug(SlugUtil.generateSlug(title));
		form.setCategoryId(categoryId);
		form.setRepoOwner(repo.getRepoOwner());
		form.setRepoName(repo.getRepoName());

		model.addAttribute("form", form);

		System.out.println("preview slug=[" + form.getSlug() + "]");

		return "publish_preview";
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

		System.out.println("executePublish start");
		System.out.println("slug=[" + slug + "]");

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
		// 非同期処理を呼び出す（ここでは即座にリダイレクト）
		// 非同期処理を呼び出す
		gitHubPushService.pushArticleAsync(
				repo,
				loginUser.getCipherKey(),
				categoryId,
				title,
				content,
				slug,
				workId,
				userId);
		return "redirect:/article/list?published";
	}
}