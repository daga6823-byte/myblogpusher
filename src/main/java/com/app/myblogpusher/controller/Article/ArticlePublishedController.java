package com.app.myblogpusher.controller.Article;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Github.GitHubArticleService;
import com.app.myblogpusher.service.Image.ImageAssetService;

import jakarta.servlet.http.HttpSession;

/**
 * 投稿済み記事機能を担当するコントローラー
 * GitHub リポジトリから取得した投稿済み記事の一覧・編集・削除を管理
 */
@Controller
public class ArticlePublishedController {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private ImageAssetService imageAssetService;

	@Autowired
	private GitHubArticleService gitHubArticleService;
	
	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	/**
	 * 投稿済み記事一覧を表示
	 */
	@GetMapping("/article/published")
	public String publishedList(
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		List<Article> articles = articleService.findPublishedByUserId(userId);

		model.addAttribute(
				"articles",
				articles);

		model.addAttribute(
				"imageCategories",
				imageAssetService.findImageCategories(userId));

		return "article/article_published_list";
	}

	@GetMapping("/article/published/edit")
	public String editPublished(
			@RequestParam String slug,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		Article article = articleService.findBySlug(userId, slug);

		if (article == null) {
			return "redirect:/article/published";
		}

		Optional<ArticleWork> existing = articleWorkService.findBySlug(slug);

		Long workId;

		if (existing.isPresent()) {

			workId = existing.get().getWorkId();

		} else {

			workId = articleWorkService.insertArticleWork(
					userId,
					article.getArticleId(),
					article.getCategoryGroupId(),
					article.getTitle(),
					article.getContent(),
					article.getSlug());
		}

		return "redirect:/article/edit?workId=" + workId;
	}

	@PostMapping("/article/published/delete")
	public String delete(
			@RequestParam String hugoPath,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		Article article = articleService.findByHugoPath(
				userId,
				hugoPath);

		if (article == null) {
			return "redirect:/article/published";
		}

		try {

			UserRepositoryEntity repo = userRepositoryRepository
					.findByUserId(userId)
					.orElseThrow();

			String cipherKey = loginUser.getCipherKey();

			gitHubArticleService.deletePublishedArticle(
					repo,
					cipherKey,
					hugoPath);

			articleService.deleteById(
					article.getArticleId());

		} catch (IOException e) {

			System.err.println(
					"投稿済み記事の削除に失敗しました: "
							+ e.getMessage());
		}

		return "redirect:/article/published";
	}
}