package com.app.myblogpusher.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.PublishPreviewForm;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.ArticleWorkRepository;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.GitHubPushService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PublishController {

	private final ArticleWorkRepository articleWorkRepository;
	private final UserRepositoryRepository userRepositoryRepository;
	private final GitHubPushService gitHubPushService;

	public PublishController(ArticleWorkRepository articleWorkRepository,
			UserRepositoryRepository userRepositoryRepository,
			GitHubPushService gitHubPushService) {
		this.articleWorkRepository = articleWorkRepository;
		this.userRepositoryRepository = userRepositoryRepository;
		this.gitHubPushService = gitHubPushService;
	}

	@GetMapping("/publish/preview")
	public String showPreview(@RequestParam Long workId, HttpSession session, Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// 記事取得
		Optional<ArticleWork> articleOpt = articleWorkRepository.findById(workId);
		if (articleOpt.isEmpty()) {
			model.addAttribute("error", "記事が見つかりません");
			return "error";
		}

		ArticleWork article = articleOpt.get();

		// ユーザー確認（他ユーザーの記事を投稿されない対策）
		if (!article.getUserId().equals(userId)) {
			model.addAttribute("error", "権限がありません");
			return "error";
		}

		// リポジトリ情報取得
		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);
		if (repoOpt.isEmpty()) {
			model.addAttribute("error", "リポジトリが設定されていません");
			return "error";
		}

		UserRepositoryEntity repo = repoOpt.get();

		PublishPreviewForm form = new PublishPreviewForm();
		form.setArticleId(workId);
		form.setArticleTitle(article.getTitle());
		form.setArticleContent(article.getContent());
		form.setRepoOwner(repo.getRepoOwner());
		form.setRepoName(repo.getRepoName());

		model.addAttribute("form", form);
		return "publish_preview";
	}

	@PostMapping("/publish/execute")
	public String executePublish(@RequestParam Long workId, HttpSession session, Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// 記事取得
		Optional<ArticleWork> articleOpt = articleWorkRepository.findById(workId);
		if (articleOpt.isEmpty()) {
			model.addAttribute("error", "記事が見つかりません");
			return "error";
		}

		ArticleWork article = articleOpt.get();

		// ユーザー確認
		if (!article.getUserId().equals(userId)) {
			model.addAttribute("error", "権限がありません");
			return "error";
		}

		// リポジトリ情報取得
		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);
		if (repoOpt.isEmpty()) {
			model.addAttribute("error", "リポジトリが設定されていません");
			return "error";
		}

		UserRepositoryEntity repo = repoOpt.get();

		try {
			gitHubPushService.pushArticle(
					repo,
					loginUser.getCipherKey(),
					article.getTitle(),
					article.getContent());
			return "redirect:/article/list?published";
		} catch (Exception e) {
			model.addAttribute("error", "投稿に失敗しました: " + e.getMessage());
			return "error";
		}
	}
}