/**
 * セッション維持処理を担当するコントローラー
 *
 * ログイン状態を確認し、セッション維持リクエストを受け付ける。
 */
package com.app.myblogpusher.controller.Article;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleSessionController {

	/**
	 * ログインセッションの維持
	 *
	 * セッションが無い場合は新規作成せず、401を返す。
	 * セッションの有効期限は、このリクエストを受けた時点で延長される。
	 */
	@PostMapping("/article/session/keepalive")
	@ResponseBody
	public ResponseEntity<Void> keepAlive(HttpServletRequest request) {

		HttpSession session = request.getSession(false);

		if (session == null || session.getAttribute("loginUser") == null) {
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.build();
		}

		return ResponseEntity.ok().build();
	}
}