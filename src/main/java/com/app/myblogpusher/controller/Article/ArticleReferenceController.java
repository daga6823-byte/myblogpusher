/**
 * カテゴリー経路ごとの参考文献管理を担当するController
 *
 * カテゴリー経路単位で参考文献一覧表示、
 * 登録、削除処理を管理する。
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
import com.app.myblogpusher.service.Article.ArticleReferenceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleReferenceController {

	private final ArticleReferenceService articleReferenceService;

	private final CategoryRelationRepository categoryRelationRepository;

	public ArticleReferenceController(
			ArticleReferenceService articleReferenceService,
			CategoryRelationRepository categoryRelationRepository) {

		this.articleReferenceService = articleReferenceService;
		this.categoryRelationRepository = categoryRelationRepository;
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

		List<ArticleReference> references = articleReferenceService.findByGroup(
				userId,
				groupId);

		CategoryRelation relation = categoryRelationRepository.findByGroupId(groupId)
				.stream()
				.findFirst()
				.orElseThrow();

		model.addAttribute("references", references);
		model.addAttribute("groupId", groupId);
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

		articleReferenceService.save(
				loginUser.getUserId(),
				groupId,
				referenceName,
				url);

		return "redirect:/category/reference?groupId=" + groupId;
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
	 * 指定カテゴリー経路の参考文献一覧をJSONで取得する
	 *
	 * 脚注挿入時に登録済み参考文献を選択するために使用する。
	 */
	@GetMapping("/category/reference/list")
	@ResponseBody
	public List<ArticleReference> referenceJson(
			@RequestParam Long groupId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		return articleReferenceService.findByGroup(
				loginUser.getUserId(),
				groupId);
	}

	/**
	 * カテゴリー経路から参考文献管理画面へ遷移する
	 */
	@GetMapping("/category/reference/open")
	public String openReference(
			@RequestParam Long groupId) {

		return "redirect:/category/reference?groupId=" + groupId;
	}
}