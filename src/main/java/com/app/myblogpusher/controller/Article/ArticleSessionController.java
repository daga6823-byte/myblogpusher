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

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleSessionController {

	/**
	 * ログインセッションの維持
	 */
	@PostMapping("/article/session/keepalive")
	@ResponseBody
	public ResponseEntity<Void> keepAlive(HttpSession session) {

		if (session.getAttribute("loginUser") == null) {
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.build();
		}

		// セッションからログインユーザーを取得して維持する。
		session.getAttribute("loginUser");

		return ResponseEntity.ok().build();
	}
}
