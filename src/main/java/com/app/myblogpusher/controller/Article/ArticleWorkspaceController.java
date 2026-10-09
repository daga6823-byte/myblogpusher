/**
 * 記事編集用ワークスペースの操作を担当するコントローラー
 *
 * ワークスペースの一時保存・削除を受け付ける。
 * データの保存処理はArticleWorkspaceServiceに委譲する。
 */
package com.app.myblogpusher.controller.Article;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.WorkspaceSaveRequest;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Article.ArticleWorkspaceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleWorkspaceController {

	@Autowired
	private ArticleWorkspaceService workspaceService;

	/**
	 * ワークスペースを一時保存
	 */
	@PostMapping("/article/workspace/save")
	@ResponseBody
	public ResponseEntity<Void> saveWorkspace(
			@RequestBody WorkspaceSaveRequest req,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.build();
		}

		workspaceService.save(
				loginUser.getUserId(),
				req.getCategoryGroupId(),
				req.getTitle(),
				req.getContent());

		return ResponseEntity.ok().build();
	}

	/**
	 * ワークスペースを削除
	 */
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
