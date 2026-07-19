/**
 * カテゴリー参考文献管理を担当するController
 *
 * カテゴリーごとの参考文献一覧表示、
 * 登録、削除処理を管理する。
 */

package com.app.myblogpusher.controller.Category;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.entity.ArticleReference;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleReferenceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CategoryReferenceController {

	private final ArticleReferenceService articleReferenceService;

	public CategoryReferenceController(
			ArticleReferenceService articleReferenceService,
			ArticleCategoryService articleCategoryService) {

		this.articleReferenceService = articleReferenceService;
	}

	/**
	 * 参考文献管理画面表示
	 */
	@GetMapping("/category/reference")
	public String referenceList(
			@RequestParam Long categoryId,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		List<ArticleReference> references = articleReferenceService.findByCategory(
				userId,
				categoryId);

		model.addAttribute(
				"references",
				references);

		model.addAttribute(
				"categoryId",
				categoryId);

		model.addAttribute(
				"categoryName",
				articleReferenceService.getReferenceCategoryName(categoryId));

		return "category/reference_list";
	}

	/**
	 * 参考文献登録
	 */
	@PostMapping("/category/reference/save")
	public String save(
			@RequestParam Long categoryId,
			@RequestParam String referenceName,
			@RequestParam(required = false) String url,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long referenceCategoryId = articleReferenceService.resolveReferenceCategoryId(categoryId);

		articleReferenceService.save(
				loginUser.getUserId(),
				referenceCategoryId,
				referenceName,
				url);

		return "redirect:/category/reference?categoryId="
				+ referenceCategoryId;
	}

	/**
	 * 参考文献削除
	 */
	@PostMapping("/category/reference/delete")
	public String delete(
			@RequestParam Long referenceId,
			@RequestParam Long categoryId) {

		articleReferenceService.delete(
				referenceId);

		return "redirect:/category/reference?categoryId=" + categoryId;
	}

	/**
	 * 指定カテゴリーの参考文献一覧をJSONで取得する
	 *
	 * 脚注挿入時に登録済み参考文献を選択するために使用する。
	 */
	@GetMapping("/category/reference/list")
	@ResponseBody
	public List<ArticleReference> referenceJson(
			@RequestParam Long categoryId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long referenceCategoryId = articleReferenceService.resolveReferenceCategoryId(categoryId);

		return articleReferenceService.findByCategory(
				loginUser.getUserId(),
				referenceCategoryId);
	}

	/**
	 * カテゴリーから参考文献管理画面へ遷移する
	 *
	 * 記事カテゴリーが深い階層の場合、
	 * 登録対象カテゴリー（ルート直下）へ補正する。
	 */
	@GetMapping("/category/reference/open")
	public String openReference(
			@RequestParam Long categoryId) {

		Long referenceCategoryId = articleReferenceService.resolveReferenceCategoryId(categoryId);

		return "redirect:/category/reference?categoryId="
				+ referenceCategoryId;
	}
}