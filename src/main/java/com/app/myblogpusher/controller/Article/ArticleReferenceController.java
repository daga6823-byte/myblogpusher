/**
 * カテゴリーを基準とした参考文献管理を担当するController
 *
 * 記事のカテゴリー経路(groupId)を入口として、
 * 参考文献を共有するカテゴリーのcategoryIdへ変換して管理する。
 *
 * 参考文献そのものはカテゴリー経路ではなく、
 * 共通カテゴリー（例：batman）に紐付ける。
 *
 * 参考文献管理画面のカテゴリー選択には、
 * 実際に参考文献が登録されているカテゴリーだけを表示する。
 */

package com.app.myblogpusher.controller.Article;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.entity.Article.ArticleReference;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.service.CategoryPathService;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Article.ArticleReferenceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleReferenceController {

	private final ArticleReferenceService articleReferenceService;

	private final ArticleCategoryService articleCategoryService;

	private final CategoryRelationRepository categoryRelationRepository;

	private final CategoryPathService categoryPathService;

	public ArticleReferenceController(
			ArticleReferenceService articleReferenceService,
			ArticleCategoryService articleCategoryService,
			CategoryRelationRepository categoryRelationRepository,
			CategoryPathService categoryPathService) {

		this.articleReferenceService = articleReferenceService;
		this.articleCategoryService = articleCategoryService;
		this.categoryRelationRepository = categoryRelationRepository;
		this.categoryPathService = categoryPathService;
	}

	/**
	 * 参考文献管理画面表示
	 */
	@GetMapping("/category/reference")
	public String referenceList(
			@RequestParam Long groupId,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		// 深いカテゴリー経路から参考文献管理用の経路へ変換する。
		Long referenceGroupId = categoryPathService.resolveReferenceGroupId(groupId);

		CategoryRelation relation = categoryRelationRepository.findByGroupId(referenceGroupId)
				.stream()
				.findFirst()
				.orElseThrow();

		// 参考文献は経路ではなく、
		// その経路が属する共通カテゴリーに紐付けて取得する。
		Long referenceCategoryId = categoryPathService.findReferenceCategoryIdByGroupId(
				referenceGroupId);

		List<ArticleReference> references = articleReferenceService.findByCategory(
				userId,
				referenceCategoryId);

		// 実際に参考文献が登録されているカテゴリーだけを取得する。
		List<Long> referenceCategoryIds = articleReferenceService.findRegisteredCategoryIds(userId);

		List<ArticleCategory> referenceCategories = referenceCategoryIds.stream()
				.map(articleCategoryService::findById)
				.flatMap(java.util.Optional::stream)
				.toList();

		model.addAttribute("references", references);
		model.addAttribute("groupId", referenceGroupId);
		model.addAttribute(
				"categoryName",
				relation.getCategoryPath());

		model.addAttribute(
				"referenceCategories",
				referenceCategories);

		return "category/reference_list";
	}

	/**
	 * 参考文献登録
	 */
	@PostMapping("/category/reference/save")
	public String save(
			@RequestParam Long groupId,
			@RequestParam String referenceName,
			@RequestParam(required = false) String url,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		// 現在の記事カテゴリーのgroupIdから、
		// 参考文献を共有するカテゴリーのcategoryIdを取得する。
		Long referenceCategoryId = categoryPathService.findReferenceCategoryIdByGroupId(
				groupId);

		articleReferenceService.save(
				loginUser.getUserId(),
				referenceCategoryId,
				referenceName,
				url);

		// 管理画面へ戻す際は、表示用のgroupIdを使用する。
		Long referenceGroupId = categoryPathService.resolveReferenceGroupId(groupId);

		return "redirect:/category/reference?groupId="
				+ referenceGroupId;
	}

	/**
	 * 参考文献削除
	 */
	@PostMapping("/category/reference/delete")
	public String delete(
			@RequestParam Long referenceId,
			@RequestParam Long groupId) {

		articleReferenceService.delete(referenceId);

		return "redirect:/category/reference?groupId=" + groupId;
	}

	/**
	 * 登録済み参考文献をJSONで取得する
	 *
	 * ログインユーザーが登録している参考文献を全カテゴリーから取得する。
	 * 画面側のreferenceCategorySelectでcategoryIdを使って絞り込む。
	 */
	@GetMapping("/category/reference/list")
	@ResponseBody
	public List<ArticleReference> referenceJson(
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		return articleReferenceService.findAll(
				loginUser.getUserId());
	}

	/**
	 * カテゴリー経路から参考文献管理画面へ遷移する
	 */
	@GetMapping("/category/reference/open")
	public String openReference(
			@RequestParam Long groupId) {

		// 深いカテゴリーから開いた場合も、
		// ルート直下のカテゴリーを参考文献管理単位とする。
		Long referenceGroupId = categoryPathService.resolveReferenceGroupId(groupId);

		return "redirect:/category/reference?groupId="
				+ referenceGroupId;
	}

	/**
	 * 参考文献が登録されているカテゴリー一覧を取得する
	 *
	 * article_referenceに実際に登録されているcategoryIdだけを対象とする。
	 * カテゴリー階層やCategoryRelationから候補を生成しない。
	 */
	@GetMapping("/category/reference/categories")
	@ResponseBody
	public List<ArticleCategory> referenceCategories(
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		List<Long> categoryIds = articleReferenceService.findRegisteredCategoryIds(
				loginUser.getUserId());

		return categoryIds.stream()
				.map(articleCategoryService::findById)
				.flatMap(java.util.Optional::stream)
				.toList();
	}
}