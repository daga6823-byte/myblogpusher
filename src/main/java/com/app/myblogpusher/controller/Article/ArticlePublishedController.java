package com.app.myblogpusher.controller.Article;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.Publish.PublishedArticleDto;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Category.CategoryPathService;
import com.app.myblogpusher.service.Facade.ImageAssetFacadeService;
import com.app.myblogpusher.service.Github.GitHubArticleService;

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
	private ImageAssetFacadeService imageAssetFacadeService;

	@Autowired
	private GitHubArticleService gitHubArticleService;

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	@Autowired
	private CategoryPathService categoryPathService;

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

		/*
		 * 投稿済み記事のカテゴリー経路を一括取得する。
		 *
		 * 記事ごとのDB検索を避け、取得したカテゴリー経路を
		 * articleIdごとに一覧表示用のマップへ変換する。
		 */
		List<Long> groupIds = articles.stream()
				.map(Article::getCategoryGroupId)
				.filter(java.util.Objects::nonNull)
				.distinct()
				.toList();

		Map<Long, String> categoryPaths = categoryPathService.findCategoryPathsByGroupIds(groupIds);

		model.addAttribute(
				"articleCategories",
				articles.stream()
						.collect(
								Collectors.toMap(
										Article::getArticleId,
										article -> categoryPaths.get(
												article.getCategoryGroupId()),
										(existing, replacement) -> existing)));

		model.addAttribute(
				"articles",
				articles);

		model.addAttribute(
				"imageCategories",
				imageAssetFacadeService.findImageCategories(userId));

		return "article/article_published_list";
	}

	@GetMapping("/article/published/edit")
	public String editPublished(
			@RequestParam String hugoPath,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		Article article = articleService.findByHugoPath(
				userId,
				hugoPath);

		Long workId;

		if (article != null) {

			Optional<ArticleWork> existing = articleWorkService.findByArticleId(
					article.getArticleId());

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

		} else {

			/*
			 * Articleテーブルに存在しない記事でも、
			 * GitHub上に存在する記事なら編集画面へ入れるようにする。
			 *
			 * 同期時にカテゴリー解決に失敗した記事を想定している。
			 * この段階ではArticleを新規作成せず、
			 * GitHubの記事内容をArticleWorkへ退避して編集を開始する。
			 */
			UserRepositoryEntity repo = userRepositoryRepository
					.findByUserId(userId)
					.orElseThrow();

			String cipherKey = loginUser.getCipherKey();

			PublishedArticleDto githubArticle;

			try {

				githubArticle = gitHubArticleService.getPublishedArticle(
						repo,
						cipherKey,
						null,
						hugoPath);

			} catch (IOException e) {

				System.err.println(
						"GitHubから投稿済み記事の取得に失敗しました: "
								+ e.getMessage());

				return "redirect:/article/published";
			}

			if (githubArticle == null) {
				return "redirect:/article/published";
			}

			workId = articleWorkService.insertArticleWork(
					userId,
					null,
					null,
					githubArticle.getTitle(),
					githubArticle.getContent(),
					githubArticle.getSlug());
		}

		String categoryPath = hugoPath;

		int lastSlash = hugoPath.lastIndexOf("/");

		if (lastSlash > 0) {
			categoryPath = hugoPath.substring(0, lastSlash);
		}

		return "redirect:/article/edit?workId="
				+ workId
				+ "&categoryPath="
				+ URLEncoder.encode(
						categoryPath,
						StandardCharsets.UTF_8);
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