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

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.PublishedArticleService;
import com.app.myblogpusher.service.PublishedArticleService.PublishedArticleDto;
import com.app.myblogpusher.util.FrontMatterUtil;

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
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private FrontMatterUtil frontMatterUtil;

	/**
	 * 投稿済み記事一覧を表示
	 */
	@GetMapping("/article/published")
	public String publishedList(HttpSession session, Model model) {
	    UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
	    Long userId = loginUser.getUserId();
	    String cipherKey = loginUser.getCipherKey(); // ここで取得

	    Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);
	    if (repoOpt.isEmpty()) {
	    	System.err.println("Repository not found");
	        model.addAttribute("error", "リポジトリが設定されていません");
	        return "error";
	    }

	    try {
	    	System.out.println("Fetching articles...");
	        List<PublishedArticleDto> articles = publishedArticleService.getPublishedArticles(repoOpt.get(), cipherKey);
	        System.out.println("Articles count: " + articles.size());
	        model.addAttribute("articles", articles);
	    } catch (IOException e) {
	        System.err.println("記事取得エラー: " + e.getMessage());
	        e.printStackTrace();
	        model.addAttribute("error", "記事の取得に失敗しました");
	    }

	    return "article/article_published_list";
	}
	
	@GetMapping("/article/published/edit")
	public String editPublished(@RequestParam String slug, HttpSession session, Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();
		String cipherKey = loginUser.getCipherKey();

		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);
		if (repoOpt.isEmpty()) {
			return "redirect:/article/list";
		}

		try {
			PublishedArticleService.PublishedArticleDto article = 
				publishedArticleService.getPublishedArticle(repoOpt.get(), cipherKey, slug);
			
			if (article == null) {
				return "redirect:/article/published";
			}

			// カテゴリを取得
			List<String> categories = frontMatterUtil.extractCategories(article.getContent());
			Long categoryId = null;
			if (!categories.isEmpty()) {
				String categoryName = categories.get(0);
				categoryId = articleCategoryService.findByUserIdAndName(userId, categoryName)
					.map(ArticleCategory::getCategoryId)
					.orElseGet(() -> articleCategoryService.insertCategory(userId, categoryName));
			}

			// article_work に保存
			Long workId = articleWorkService.insertArticleWork(
				userId,
				categoryId,
				article.getTitle(),
				article.getContent(),
				slug);

			return "redirect:/article/edit?workId=" + workId;
		} catch (IOException e) {
			return "redirect:/article/published";
		}
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