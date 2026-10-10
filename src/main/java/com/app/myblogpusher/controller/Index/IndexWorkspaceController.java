/**
 * インデックス編集中の自動保存を担当するコントローラー
 *
 * 編集内容をindex_workspaceへ自動保存する。
 * 手動保存やGitHubへの投稿処理は担当しない。
 */
package com.app.myblogpusher.controller.Index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Index.IndexEditService;
import com.app.myblogpusher.service.Index.IndexWorkspaceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexWorkspaceController {

	@Autowired
	private IndexEditService indexEditService;

	@Autowired
	private IndexWorkspaceService indexWorkspaceService;

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
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}

		// 編集対象のカテゴリー階層がユーザー所有か確認する。
		boolean ownsCategoryPath = indexEditService
				.findCategoryPaths(loginUser.getUserId())
				.stream()
				.anyMatch(category -> category.getGroupId().equals(groupId));

		if (!ownsCategoryPath) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"指定されたカテゴリー階層を編集する権限がありません。");
		}

		// フロントマターのタイトルと本文を自動保存する。
		indexWorkspaceService.save(
				loginUser.getUserId(),
				groupId,
				extractFrontMatterValue(content, "title"),
				content);

		return "OK";
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