/**
 * ログイン・パスワードリセット機能を担当するコントローラー
 * ログイン成功時に投稿済み記事一覧を非同期で先読みし、記事一覧画面の表示を高速化する
 */

package com.app.myblogpusher.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.ArticleWorkspaceService;
import com.app.myblogpusher.service.LoginService;
import com.app.myblogpusher.service.PublishedArticleService;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

	@Autowired
	private LoginService loginService;

	@Autowired
	private ArticleWorkspaceService workspaceService;

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	@Autowired
	private PublishedArticleService publishedArticleService;

	@GetMapping("/login")
	public String loginForm() {
		return "login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String loginId,
			@RequestParam String password,
			HttpSession session,
			Model model) {
		Optional<UserMaster> userOpt = loginService.findAuthenticatedUser(loginId, password);
		if (userOpt.isPresent()) {
			UserMaster user = userOpt.get();

			// ログイン前に前のセッションのワークスペースをクリア
			workspaceService.delete(user.getUserId());

			// 新しいセッションを設定
			session.setAttribute("loginUser", user);

			// 投稿済み記事一覧を非同期で先読みし、記事一覧画面の表示を高速化する
			userRepositoryRepository.findByUserId(user.getUserId())
					.ifPresent(repo -> publishedArticleService.prefetchPublishedArticles(
							repo,
							user.getCipherKey(),
							session));

			return "redirect:/home";
		} else {
			model.addAttribute("error", "ログインIDまたはパスワードが間違っています");
			return "login";
		}
	}

	// パスワードを忘れた方はこちら（本人確認フォーム表示）
	@GetMapping("/login/forgot")
	public String forgotPasswordForm(Model model) {
		model.addAttribute("mode", "forgot");
		return "forgot_password";
	}

	// 本人確認
	@PostMapping("/login/forgot")
	public String verifyForReset(@RequestParam String loginId,
			@RequestParam String email,
			HttpSession session,
			Model model) {
		Optional<UserMaster> userOpt = loginService.findUserForPasswordReset(loginId, email);
		if (userOpt.isEmpty()) {
			model.addAttribute("mode", "forgot");
			model.addAttribute("error", "入力内容に一致するユーザーが見つかりませんでした");
			return "forgot_password";
		}
		session.setAttribute("passwordResetUserId", userOpt.get().getUserId());
		model.addAttribute("mode", "reset");
		return "forgot_password";
	}

	// 新パスワード設定
	@PostMapping("/login/reset")
	public String resetPassword(@RequestParam String newPassword,
			@RequestParam String newPasswordConfirm,
			HttpSession session,
			Model model) {
		Long userId = (Long) session.getAttribute("passwordResetUserId");
		if (userId == null) {
			model.addAttribute("mode", "forgot");
			return "forgot_password";
		}
		if (newPassword == null || newPassword.isBlank()) {
			model.addAttribute("mode", "reset");
			model.addAttribute("error", "新しいパスワードを入力してください");
			return "forgot_password";
		}
		if (!newPassword.equals(newPasswordConfirm)) {
			model.addAttribute("mode", "reset");
			model.addAttribute("error", "確認用のパスワードが一致しません");
			return "forgot_password";
		}
		boolean success = loginService.resetPassword(userId, newPassword);
		session.removeAttribute("passwordResetUserId");
		if (!success) {
			model.addAttribute("mode", "forgot");
			model.addAttribute("error", "セッションが無効です。お手数ですが再度お試しください");
			return "forgot_password";
		}
		model.addAttribute("mode", "done");
		return "forgot_password";
	}
}