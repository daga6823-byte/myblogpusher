package com.app.myblogpusher.controller.Article;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.ArticleService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.PublishedArticleService;

import jakarta.servlet.http.HttpSession;

/**
 * 投稿済み記事機能を担当するコントローラー
 * GitHub リポジトリから取得した投稿済み記事の一覧・編集・削除を管理
 */
@Controller
public class ArticlePublishedController {

	@Autowired
	private PublishedArticleService publishedArticleService;

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleService articleService;
	
	/**
	 * 投稿済み記事一覧を表示
	 */
	@GetMapping("/article/published")
	public String publishedList(
			HttpSession session,
			Model model) {

		UserMaster loginUser =
				(UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		String cipherKey = loginUser.getCipherKey();

		Optional<UserRepositoryEntity> repoOpt =
				userRepositoryRepository.findByUserId(userId);

		if (repoOpt.isPresent()) {

			publishedArticleService.syncArticles(
					repoOpt.get(),
					cipherKey,
					userId);
		}

		List<Article> articles =
				articleService.findPublishedByUserId(userId);

		model.addAttribute(
				"articles",
				articles);

		return "article/article_published_list";
	}

	@GetMapping("/article/published/edit")
	public String editPublished(
			@RequestParam String slug,
			HttpSession session) {

		UserMaster loginUser =
				(UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		Article article =
				articleService.findBySlug(userId, slug);

		if (article == null) {
			return "redirect:/article/published";
		}

		Optional<ArticleWork> existing =
				articleWorkService.findBySlug(slug);

		Long workId;

		if (existing.isPresent()) {

			workId = existing.get().getWorkId();

		} else {

			workId = articleWorkService.insertArticleWork(
					userId,
					article.getCategoryId(),
					article.getTitle(),
					article.getContent(),
					article.getSlug());
		}

		return "redirect:/article/edit?workId=" + workId;
	}

	/**
	 * 投稿済み記事を削除
	 */
	@PostMapping("/article/published/delete")
	public String delete(@RequestParam String slug, HttpSession session) {
		// TODO: GitHub から削除処理を実装
		return "redirect:/article/published";
	}
}