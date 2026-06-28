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
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.PublishedArticleService;
import com.app.myblogpusher.service.PublishedArticleService.PublishedArticleDto;

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

	/**
	 * 投稿済み記事一覧を表示
	 */
	@GetMapping("/article/published")
	public String publishedList(HttpSession session, Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);
		if (repoOpt.isEmpty()) {
			model.addAttribute("error", "リポジトリが設定されていません");
			return "error";
		}

		try {
			List<PublishedArticleDto> articles = publishedArticleService.getPublishedArticles(repoOpt.get().getRepoId());
			model.addAttribute("articles", articles);
		} catch (IOException e) {
			model.addAttribute("error", "記事の取得に失敗しました");
		}

		return "article/article_published_list";
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