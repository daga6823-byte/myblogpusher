/**
 * 記事編集機能を担当するコントローラー
 * 編集画面表示、下書き保存、添削処理を管理
 */

package com.app.myblogpusher.controller.Article;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.WorkspaceSaveRequest;
import com.app.myblogpusher.dto.Article.ArticleLinkView;
import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Article.ArticleWorkspaceService;
import com.app.myblogpusher.service.Image.ImageAssetService;
import com.app.myblogpusher.util.ArticleSaveUtil;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleEditController {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleWorkspaceService workspaceService;

	@Autowired
	private ArticleSaveUtil articleSaveUtil;

	@Autowired
	private ImageAssetService imageAssetService;

	@Autowired
	private ArticleRepository articleRepository;

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

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

		UserRepositoryEntity repo = userRepositoryRepository.findByUserId(userId)
				.orElse(null);

		// カテゴリー選択プルダウン用（categoryId + フルパス表示）
		List<CategoryOptionView> categories = articleCategoryService.findSelectableCategories(userId);

		categories.forEach(c -> System.out.println(c.getCategoryId() + " : " + c.getFullPath()));

		model.addAttribute("categories", categories);

		// 画像モーダル用
		// 登録済み画像の保存先フォルダ一覧を取得
		model.addAttribute(
				"imageFolders",
				imageAssetService.findImageCategories(userId));

		ArticleWork work = null;

		if (workId != null) {

			work = articleWorkService.findById(workId);

			model.addAttribute("work", work);
			model.addAttribute("categoryId", work.getCategoryId());

		}

		if (work != null && work.getArticleId() != null) {

			Article article = articleRepository.findById(work.getArticleId())
					.orElse(null);

			if (article != null) {

				System.out.println("articleId=" + work.getArticleId());
				System.out.println("createDate=" + article.getCreateDate());

				model.addAttribute(
						"createDate",
						article.getCreateDate());

				model.addAttribute(
						"updateDate",
						article.getUpdateDate());
			}

		} else if (work != null) {

			model.addAttribute(
					"createDate",
					work.getCreateDate());

			model.addAttribute(
					"updateDate",
					work.getUpdateDate());
		}

		// -----------------------------------------------------
		// 記事リンク挿入用
		//
		// 編集中記事のカテゴリーを基準に検索する。
		// GitHub APIは使用しない。
		// -----------------------------------------------------

		List<Article> articles = List.of();

		Long currentCategoryId = null;

		// 編集中記事
		if (workId != null) {

			currentCategoryId = work.getCategoryId();

		}

		// workspace復元時
		else {

			currentCategoryId = workspaceService.find(userId)
					.map(ws -> ws.getCategoryId())
					.orElse(null);

		}

		if (currentCategoryId != null) {

			Long searchCategoryId = articleCategoryService
					.findLinkSearchCategoryId(currentCategoryId);

			if (searchCategoryId != null) {

				ArticleCategory searchCategory = articleCategoryService
						.findById(searchCategoryId)
						.orElse(null);

				if (searchCategory != null) {

					String searchPath = articleCategoryService
							.findLinkSearchCategoryPath(currentCategoryId);

					if (searchPath != null) {

						articles = articleRepository.findLinkArticles(
								userId,
								searchPath);

					}

				}

			}

		}

		List<ArticleLinkView> linkArticles = articles.stream()
				.map(article -> new ArticleLinkView(
						article.getSlug(),
						article.getHugoPath(),
						article.getTitle(),
						"https://" + repo.getRepoName().toLowerCase()
								+ "/" + article.getHugoPath()
								+ "/"))
				.collect(Collectors.toList());

		model.addAttribute(
				"publishedArticles",
				linkArticles);

		model.addAttribute(
				"linkCategories",
				articleCategoryService.findSelectableCategories(userId));

		model.addAttribute(
				"linkSearchCategoryId",
				currentCategoryId != null
						? articleCategoryService.findLinkSearchCategoryId(currentCategoryId)
						: null);

		model.addAttribute(
				"siteUrl",
				repo != null
						? "https://" + repo.getRepoName().toLowerCase()
						: "");

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

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		Long savedWorkId = articleSaveUtil.doSaveDraft(workId, categorySelect, newCategoryName, title, content, userId);

		if (savedWorkId == null) {
			return "redirect:/article/edit";
		}

		if ("home".equals(redirectTo)) {
			return "redirect:/home";
		}
		if ("list".equals(redirectTo)) {
			return "redirect:/article/list";
		}

		// 下書き保存後は一時ワークスペースを削除
		workspaceService.delete(userId);

		return "redirect:/article/edit?workId=" + savedWorkId + "&saved=true";
	}

	@PostMapping("/article/workspace/save")
	@ResponseBody
	public ResponseEntity<Void> saveWorkspace(
			@RequestBody WorkspaceSaveRequest req,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		workspaceService.save(
				loginUser.getUserId(),
				req.getCategoryId(),
				req.getTitle(),
				req.getContent());

		return ResponseEntity.ok().build();
	}

	@PostMapping("/article/session/keepalive")
	@ResponseBody
	public ResponseEntity<Void> keepAlive(HttpSession session) {

		if (session.getAttribute("loginUser") == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		session.getAttribute("loginUser");

		return ResponseEntity.ok().build();
	}

	@PostMapping("/article/workspace/clear")
	@ResponseBody
	public ResponseEntity<Void> clearWorkspace(HttpSession session) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		if (loginUser != null) {
			workspaceService.delete(loginUser.getUserId());
		}
		return ResponseEntity.ok().build();
	}

}