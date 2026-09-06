/**
 * カテゴリー経路ごとの参考文献管理を担当するController
 *
 * カテゴリー経路単位で参考文献一覧表示、
 * 登録、削除処理を管理する。
 *
 * 参考文献はルートカテゴリー直下のカテゴリー単位で管理するため、
 * 記事側の深いカテゴリー経路が指定された場合は、
 * CategoryRelationServiceで参考文献管理用groupIdへ変換する。
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
import com.app.myblogpusher.service.CategoryRelationService;
import com.app.myblogpusher.service.Article.ArticleReferenceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleReferenceController {

	private final ArticleReferenceService articleReferenceService;

	private final CategoryRelationRepository categoryRelationRepository;

	private final CategoryRelationService categoryRelationService;

	public ArticleReferenceController(
			ArticleReferenceService articleReferenceService,
			CategoryRelationRepository categoryRelationRepository,
			CategoryRelationService categoryRelationService) {

		this.articleReferenceService = articleReferenceService;
		this.categoryRelationRepository = categoryRelationRepository;
		this.categoryRelationService = categoryRelationService;
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

		// 指定されたカテゴリー経路を参考文献管理単位へ変換する。
		Long referenceGroupId = categoryRelationService.resolveReferenceGroupId(groupId);

		List<ArticleReference> references = articleReferenceService.findByGroup(
				userId,
				referenceGroupId);

		CategoryRelation relation = categoryRelationRepository.findByGroupId(referenceGroupId)
				.stream()
				.findFirst()
				.orElseThrow();

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

		// 念のため、登録時も参考文献管理単位へ変換する。
		Long referenceGroupId = categoryRelationService.resolveReferenceGroupId(groupId);

		articleReferenceService.save(
				loginUser.getUserId(),
				referenceGroupId,
				referenceName,
				url);

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

		// 記事の深いカテゴリー経路を参考文献管理単位へ変換する。
		Long referenceGroupId = categoryRelationService.resolveReferenceGroupId(groupId);

		return articleReferenceService.findByGroup(
				loginUser.getUserId(),
				referenceGroupId);
	}

	/**
	 * カテゴリー経路から参考文献管理画面へ遷移する
	 */
	@GetMapping("/category/reference/open")
	public String openReference(
			@RequestParam Long groupId) {

		// 深いカテゴリーから開いた場合も、
		// ルート直下のカテゴリーを参考文献管理単位とする。
		Long referenceGroupId = categoryRelationService.resolveReferenceGroupId(groupId);

		return "redirect:/category/reference?groupId="
				+ referenceGroupId;
	}
}