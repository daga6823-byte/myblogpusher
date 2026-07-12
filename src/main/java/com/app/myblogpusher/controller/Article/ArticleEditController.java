/**
 * 記事編集機能を担当するコントローラー
 * 編集画面表示、下書き保存、添削処理を管理
 */

package com.app.myblogpusher.controller.Article;

import java.util.List;

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

import com.app.myblogpusher.dto.CategoryOptionView;
import com.app.myblogpusher.dto.WorkspaceSaveRequest;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.ArticleWorkspaceService;
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

		// カテゴリー選択プルダウン用（categoryId + フルパス表示）
		List<CategoryOptionView> categories = articleCategoryService.findSelectableCategories(userId);
		model.addAttribute("categories", categories);

		if (workId != null) {
			ArticleWork work = articleWorkService.findById(workId);
			model.addAttribute("work", work);
			model.addAttribute("categoryId", work.getCategoryId());
		} else {
			workspaceService.find(userId)
					.ifPresent(ws -> {
						ArticleWork work = new ArticleWork();
						work.setTitle(ws.getTitle());
						work.setContent(ws.getContent());
						work.setCategoryId(ws.getCategoryId());
						model.addAttribute("work", work);
						model.addAttribute("categoryId", ws.getCategoryId());
					});
		}

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