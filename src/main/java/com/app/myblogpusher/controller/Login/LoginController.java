/**
 * ログイン・パスワードリセット機能を担当するコントローラー
 * ログイン成功時に投稿済み記事一覧を非同期で先読みし、記事一覧画面の表示を高速化する
 */

package com.app.myblogpusher.controller.Login;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.PublishedArticleSyncService;
import com.app.myblogpusher.service.Article.ArticleWorkspaceService;
import com.app.myblogpusher.service.Image.ImageAssetPreloadAsyncService;
import com.app.myblogpusher.service.Index.IndexSyncService;
import com.app.myblogpusher.service.Login.LoginHistoryService;
import com.app.myblogpusher.service.Login.LoginRegionAsyncService;
import com.app.myblogpusher.service.Login.LoginService;

import jakarta.servlet.http.HttpServletRequest;
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
	private LoginHistoryService loginHistoryService;

	@Autowired
	private LoginRegionAsyncService loginRegionAsyncService;

	@Autowired
	private ImageAssetPreloadAsyncService imageAssetPreloadAsyncService;

	@Autowired
	private PublishedArticleSyncService publishedArticleSyncService;

	@Autowired
	private IndexSyncService indexSyncService;

	@GetMapping("/login")
	public String loginForm() {
		return "Login/login";
	}

	@PostMapping("/login")
	public String login(
			@RequestParam String loginId,
			@RequestParam String password,
			HttpServletRequest request,
			HttpSession session,
			Model model) {

		long loginStart = System.nanoTime();
		long stepStart = System.nanoTime();

		Optional<UserMaster> userOpt = loginService.findAuthenticatedUser(loginId, password);

		logElapsed("ユーザー認証", stepStart);

		if (userOpt.isPresent()) {
			UserMaster user = userOpt.get();

			String ipAddress = request.getHeader("CF-Connecting-IP");
			if (ipAddress == null || ipAddress.isBlank()) {
				ipAddress = request.getRemoteAddr();
			}
			String userAgent = request.getHeader("User-Agent");

			if (user.getTwoFactorAuthenticatedAt() == null) {
				session.setAttribute("twoFactorUserId", user.getUserId());
				session.setAttribute("twoFactorIpAddress", ipAddress);
				session.setAttribute("twoFactorUserAgent", userAgent);

				logElapsed("ログイン全体（2FA設定へ遷移）", loginStart);
				return "redirect:/login/2fa/setup";
			}

			if (loginService.requiresTwoFactor(user, ipAddress)) {
				session.setAttribute("twoFactorUserId", user.getUserId());
				session.setAttribute("twoFactorIpAddress", ipAddress);
				session.setAttribute("twoFactorUserAgent", userAgent);

				logElapsed("ログイン全体（2FA認証へ遷移）", loginStart);
				return "redirect:/login/2fa";
			}

			stepStart = System.nanoTime();
			Long historyId = loginHistoryService.recordLogin(
					user.getUserId(), ipAddress, null, userAgent);
			logElapsed("ログイン履歴登録", stepStart);

			stepStart = System.nanoTime();
			loginRegionAsyncService.updateRegionAsync(historyId, ipAddress);
			logElapsed("ログイン地域更新の呼び出し", stepStart);

			session.setAttribute("loginRegionVerified", false);
			session.setAttribute("loginUser", user);

			stepStart = System.nanoTime();
			Optional<com.app.myblogpusher.entity.UserRepositoryEntity> repoOpt = userRepositoryRepository
					.findByUserId(user.getUserId());
			logElapsed("GitHubリポジトリ情報取得", stepStart);

			if (repoOpt.isPresent()) {
				var repo = repoOpt.get();

				stepStart = System.nanoTime();
				publishedArticleSyncService.syncArticles(
						repo, user.getCipherKey(), user.getUserId());
				logElapsed("記事同期の呼び出し", stepStart);

				stepStart = System.nanoTime();
				indexSyncService.syncFromGitHub(
						user.getUserId(), repo, user.getCipherKey());
				logElapsed("インデックス同期の呼び出し", stepStart);
			}

			stepStart = System.nanoTime();
			imageAssetPreloadAsyncService.preloadAsync(user.getUserId());
			logElapsed("画像先読みの呼び出し", stepStart);

			logElapsed("ログイン全体（ホームへ遷移）", loginStart);
			return "redirect:/home";

		} else {
			model.addAttribute(
					"error", "ログインIDまたはパスワードが間違っています");

			logElapsed("ログイン全体（認証失敗）", loginStart);
			return "Login/login";
		}
	}

	/**
	 * 処理の経過時間をミリ秒単位でログ出力する。
	 */
	private void logElapsed(String processName, long startNanos) {
		long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

		System.out.println(
				"[LoginTiming] " + processName + ": " + elapsedMillis + " ms");
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