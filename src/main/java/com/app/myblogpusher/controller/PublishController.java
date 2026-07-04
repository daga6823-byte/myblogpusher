/**
 * 記事投稿機能を担当するコントローラー
 * 投稿前確認画面の表示、スラッグ生成・編集、GitHubへの記事プッシュを管理
 */

package com.app.myblogpusher.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.PublishPreviewForm;
import com.app.myblogpusher.dto.SlugAnalysisDto;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.ArticleWorkRepository;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.ArticleWorkspaceService;
import com.app.myblogpusher.service.GitHubPushService;
import com.app.myblogpusher.util.SlugUtil;

import jakarta.servlet.http.HttpSession;

@Controller
public class PublishController {

	private final UserRepositoryRepository userRepositoryRepository;
	private final ArticleCategoryService articleCategoryService;
	private final GitHubPushService gitHubPushService;

	@Autowired
	private SlugUtil slugUtil;

	@Autowired
	private ArticleWorkspaceService workspaceService;

	public PublishController(ArticleWorkRepository articleWorkRepository,
			ArticleWorkService articleWorkService,
			UserRepositoryRepository userRepositoryRepository,
			ArticleCategoryService articleCategoryService,
			GitHubPushService gitHubPushService) {
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
		session.setAttribute("previewTitle", title);
		session.setAttribute("previewContent", content);
		session.setAttribute("previewWorkId", workId);
		session.setAttribute("previewCategorySelect", categorySelect);
		session.setAttribute("previewNewCategoryName", newCategoryName);

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
		form.setSlug(slugUtil.generateSlug(title));
		form.setCategoryId(categoryId);
		form.setRepoOwner(repo.getRepoOwner());
		form.setRepoName(repo.getRepoName());
		form.setSlug(slugUtil.generateSlug(title));

		List<SlugAnalysisDto> analysis = slugUtil.analyzeSlug(title);
		model.addAttribute("analysis", analysis);
		model.addAttribute("form", form);

		System.out.println("preview slug=[" + form.getSlug() + "]");

		return "publish_preview";
	}

	@PostMapping("/publish/execute")
	public String executePublish(@RequestParam(required = false) Long workId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam Long categoryId,
			HttpSession session,
			Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		System.out.println("executePublish start");

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
		String slug = gitHubPushService.generateSlugWithDictionary(title);

		gitHubPushService.pushArticleAsync(
				repo,
				loginUser.getCipherKey(),
				categoryId,
				title,
				content,
				slug,
				workId,
				userId);

		workspaceService.delete(userId);

		return "redirect:/article/list?published";
	}

	@GetMapping("/publish/preview/back")
	public String backToPreview(HttpSession session, Model model) {
		String title = (String) session.getAttribute("previewTitle");
		String content = (String) session.getAttribute("previewContent");
		Long workId = (Long) session.getAttribute("previewWorkId");
		String categorySelect = (String) session.getAttribute("previewCategorySelect");
		String newCategoryName = (String) session.getAttribute("previewNewCategoryName");

		// showPreviewと同じ処理をGETで実行
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		String categoryName = "__new__".equals(categorySelect) ? newCategoryName : categorySelect;
		Long categoryId = articleCategoryService.findByUserIdAndName(userId, categoryName)
				.map(ArticleCategory::getCategoryId)
				.orElseGet(() -> articleCategoryService.insertCategory(userId, categoryName));

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
		form.setSlug(slugUtil.generateSlug(title));
		form.setCategoryId(categoryId);
		form.setRepoOwner(repo.getRepoOwner());
		form.setRepoName(repo.getRepoName());

		List<SlugAnalysisDto> analysis = slugUtil.analyzeSlug(title);
		model.addAttribute("analysis", analysis);
		model.addAttribute("form", form);

		return "publish_preview";
	}
}