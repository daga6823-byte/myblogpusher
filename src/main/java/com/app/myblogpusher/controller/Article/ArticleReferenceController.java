/**
 * カテゴリー経路を基準とした参考文献管理を担当するController
 *
 * 記事のカテゴリー経路(groupId)を入口として、
 * 参考文献を共有するカテゴリーのcategoryIdへ変換して管理する。
 *
 * 参考文献そのものはカテゴリー経路ではなく、
 * 共通カテゴリー（例：batman）に紐付ける。
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
import com.app.myblogpusher.entity.Article.ArticleReference;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.service.CategoryPathService;
import com.app.myblogpusher.service.Article.ArticleReferenceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleReferenceController {

	private final ArticleReferenceService articleReferenceService;

	private final CategoryRelationRepository categoryRelationRepository;

	private final CategoryPathService categoryPathService;

	public ArticleReferenceController(
			ArticleReferenceService articleReferenceService,
			CategoryRelationRepository categoryRelationRepository,
			CategoryPathService categoryPathService) {

		this.articleReferenceService = articleReferenceService;
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

		model.addAttribute("references", references);
		model.addAttribute("groupId", referenceGroupId);
		model.addAttribute(
				"categoryName",
				relation.getCategoryPath());

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
	 * 指定された記事カテゴリーを基準に
	 * 参考文献一覧をJSONで取得する。
	 *
	 * 記事のgroupIdから共通カテゴリーのcategoryIdを取得し、
	 * そのカテゴリーに登録された参考文献を返却する。
	 */
	@GetMapping("/category/reference/list")
	@ResponseBody
	public List<ArticleReference> referenceJson(
			@RequestParam Long groupId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long referenceCategoryId = categoryPathService.findReferenceCategoryIdByGroupId(
				groupId);

		return articleReferenceService.findByCategory(
				loginUser.getUserId(),
				referenceCategoryId);
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
}