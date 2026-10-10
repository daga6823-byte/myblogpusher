
/**
 * インデックス編集中の一時保存を担当するコントローラー
 *
 * 編集内容の手動保存と、自動保存データの更新を担当する。
 * GitHubへの投稿処理や投稿処理用データの管理は担当しない。
 */
package com.app.myblogpusher.controller.Index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Index.IndexEditService;
import com.app.myblogpusher.service.Index.IndexWorkService;
import com.app.myblogpusher.service.Index.IndexWorkspaceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexWorkspaceController {

	@Autowired
	private IndexEditService indexEditService;

	@Autowired
	private IndexWorkspaceService indexWorkspaceService;

	@Autowired
	private IndexWorkService indexWorkService;

	/**
	 * 編集内容を手動で一時保存する。
	 */
	@PostMapping("/index/edit/save")
	public String saveIndex(
			@RequestParam Long groupId,
			@RequestParam String content,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		try {
			validateCategory(loginUser.getUserId(), groupId);

			// 編集内容をindex_workに保存する。
			indexWorkService.saveDraft(
					loginUser.getUserId(),
					groupId,
					extractFrontMatterValue(content, "title"),
					content);

			// index_workへの保存成功後に自動保存データを削除する。
			indexWorkspaceService.delete(loginUser.getUserId());

			redirectAttributes.addFlashAttribute(
					"message", "インデックスを一時保存しました。");

		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute(
					"error", "インデックスの保存に失敗しました: " + e.getMessage());
		}

		return "redirect:/index/list";
	}

	/**
	 * 編集中の内容を自動保存する。
	 *
	 * 非同期リクエストから呼び出されるため、画面遷移は行わない。
	 */
	@PostMapping("/index/workspace/autosave")
	@ResponseBody
	public String autosaveIndex(
			@RequestParam Long groupId,
			@RequestParam String content,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.UNAUTHORIZED);
		}

		validateCategory(loginUser.getUserId(), groupId);

		indexWorkspaceService.save(
				loginUser.getUserId(),
				groupId,
				extractFrontMatterValue(content, "title"),
				content);

		return "OK";
	}

	/**
	 * ログインユーザーが対象カテゴリーを編集できることを確認する。
	 */
	private void validateCategory(Long userId, Long groupId) {
		boolean ownsCategoryPath = indexEditService
				.findCategoryPaths(userId)
				.stream()
				.anyMatch(category -> category.getGroupId().equals(groupId));

		if (!ownsCategoryPath) {
			throw new IllegalArgumentException(
					"指定されたカテゴリー階層を編集する権限がありません。");
		}
	}

	/**
	 * Markdownのフロントマターから指定キーの値を取得する。
	 */
	private String extractFrontMatterValue(String markdown, String key) {
		if (markdown == null || markdown.isBlank()) {
			return null;
		}

		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
				"(?m)^" + java.util.regex.Pattern.quote(key)
						+ "\\s*:\\s*[\"']?(.*?)[\"']?\\s*$");

		java.util.regex.Matcher matcher = pattern.matcher(markdown);
		return matcher.find() ? matcher.group(1).trim() : null;
	}
}
